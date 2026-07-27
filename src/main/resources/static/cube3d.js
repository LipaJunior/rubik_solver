// Silnik 3D kostki oparty na Three.js (WebGL) - zastepuje wczesniejszy render na
// CSS 3D (transform-style: preserve-3d), ktory na Safari/Chrome migotal i dawal
// z-fighting (czarne trojkaty). WebGL ma prawdziwy bufor glebokosci, wiec ta cala
// klasa problemow znika. Logika kostki (currentCube, ruchy, malowanie) zostaje w
// script.js - tu jest wylacznie warstwa wizualna.
import * as THREE from './lib/three.module.min.js';

const COLORS = {
    W: 0xffffff, R: 0xe74c3c, B: 0x3498db,
    G: 0x2ecc71, O: 0xf39c12, Y: 0xffe000
};

const SPACING = 1.0;      // odstep miedzy srodkami cubie
const CUBIE = 0.94;       // rozmiar czarnego korpusu cubie (gap = 0.06 -> siatka)
const STICKER = 0.82;     // rozmiar kolorowej naklejki
const RADIUS = 0.12;      // zaokraglenie rogow naklejki
const HALF = CUBIE / 2 + 0.006; // naklejka tuz nad scianka korpusu

// Zaokraglony prostokat jako THREE.Shape (do naklejek).
function roundedRect(w, h, r) {
    const s = new THREE.Shape();
    const x = -w / 2, y = -h / 2;
    s.moveTo(x + r, y);
    s.lineTo(x + w - r, y); s.quadraticCurveTo(x + w, y, x + w, y + r);
    s.lineTo(x + w, y + h - r); s.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    s.lineTo(x + r, y + h); s.quadraticCurveTo(x, y + h, x, y + h - r);
    s.lineTo(x, y + r); s.quadraticCurveTo(x, y, x + r, y);
    return s;
}

// Ustawienie naklejki na danej sciance korpusu (plaszczyzna XY -> obrot na wlasciwa scianke).
const FACE_ORIENT = {
    front: m => { m.position.z = HALF; },
    back:  m => { m.position.z = -HALF; m.rotation.y = Math.PI; },
    right: m => { m.position.x = HALF; m.rotation.y = Math.PI / 2; },
    left:  m => { m.position.x = -HALF; m.rotation.y = -Math.PI / 2; },
    up:    m => { m.position.y = HALF; m.rotation.x = -Math.PI / 2; },
    down:  m => { m.position.y = -HALF; m.rotation.x = Math.PI / 2; }
};

// Czy dana scianka cubie (x,y,z) jest zewnetrzna (na powierzchni kostki).
function isOuter(x, y, z, dir) {
    return (dir === 'right' && x === 1) || (dir === 'left' && x === -1) ||
           (dir === 'up' && y === 1)    || (dir === 'down' && y === -1) ||
           (dir === 'front' && z === 1) || (dir === 'back' && z === -1);
}

export class Cube3D {
    constructor(container) {
        this.container = container;

        this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        this.renderer.setClearColor(0x000000, 0);
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        container.appendChild(this.renderer.domElement);
        this.renderer.domElement.style.touchAction = 'none'; // obracanie nie przewija strony
        this.renderer.domElement.style.display = 'block';

        this.scene = new THREE.Scene();
        // Kamera cofnieta na tyle, by kostka obrocona "na rog" (najdluzsza przekatna)
        // wciaz miescila sie z marginesem - inaczej przy obrocie obcinaloby rogi.
        this.camera = new THREE.PerspectiveCamera(30, 1, 0.1, 100);
        this.camera.position.set(5.6, 4.4, 8.1);
        this.camera.lookAt(0, 0, 0);

        // Three r155+ uzywa fizycznie poprawnego oswietlenia - intensywnosci musza
        // byc wyzsze, inaczej kolory wychodza szare/przygaszone.
        this.scene.add(new THREE.AmbientLight(0xffffff, 2.1));
        const dir = new THREE.DirectionalLight(0xffffff, 2.0);
        dir.position.set(4, 7, 6);
        this.scene.add(dir);
        const dir2 = new THREE.DirectionalLight(0xffffff, 0.7);
        dir2.position.set(-5, -3, -4);
        this.scene.add(dir2);

        this.cubeGroup = new THREE.Group();
        this.scene.add(this.cubeGroup);

        this.bodyGeo = new THREE.BoxGeometry(CUBIE, CUBIE, CUBIE);
        this.bodyMat = new THREE.MeshStandardMaterial({ color: 0x0a0a0a, roughness: 0.85, metalness: 0.0 });
        this.stickerGeo = new THREE.ShapeGeometry(roundedRect(STICKER, STICKER, RADIUS));

        this.cubies = {}; // "x,y,z" -> { mesh, home:{pos,quat}, stickers:{dir: mesh} }
        this.bodyMeshes = []; // do raycastingu (malowanie klikaniem)
        this._build();

        // Malowanie klikaniem po kostce 3D (raycasting). script.js ustawia onPick
        // i pickEnabled (gdy wlaczony tryb Paint).
        this.raycaster = new THREE.Raycaster();
        this.onPick = null;
        this.pickEnabled = false;

        this._bindPointer();
        this._raf = null;
        this._resizeObserver = new ResizeObserver(() => this.onShow());
        this._resizeObserver.observe(container);
        this.onShow();
    }

    _build() {
        for (let x = -1; x <= 1; x++)
        for (let y = -1; y <= 1; y++)
        for (let z = -1; z <= 1; z++) {
            if (x === 0 && y === 0 && z === 0) continue; // rdzen
            const mesh = new THREE.Mesh(this.bodyGeo, this.bodyMat);
            mesh.position.set(x * SPACING, y * SPACING, z * SPACING);
            mesh.userData.key = `${x},${y},${z}`;
            this.bodyMeshes.push(mesh);
            const stickers = {};
            ['front', 'back', 'right', 'left', 'up', 'down'].forEach(dir => {
                if (!isOuter(x, y, z, dir)) return;
                const st = new THREE.Mesh(
                    this.stickerGeo,
                    new THREE.MeshStandardMaterial({ color: 0x0a0a0a, roughness: 0.6, metalness: 0.0 })
                );
                FACE_ORIENT[dir](st);
                st.visible = false;
                mesh.add(st);
                stickers[dir] = st;
            });
            this.cubeGroup.add(mesh);
            this.cubies[`${x},${y},${z}`] = {
                mesh,
                home: { pos: mesh.position.clone(), quat: mesh.quaternion.clone() },
                stickers
            };
        }
    }

    // Koloruje naklejki z currentCube. Uzywa DOKLADNIE tego samego mapowania
    // (sciana,row,col) -> (x,y,z,dir) co dotychczasowy paint3DCubies (sprawdzone).
    setColors(cube) {
        if (!cube) return;
        const assign = (x, y, z, dir, faceName, row, col) => {
            const c = this.cubies[`${x},${y},${z}`];
            if (!c) return;
            const st = c.stickers[dir];
            if (!st) return;
            // Zapamietaj logiczna pozycje naklejki - raycasting przy malowaniu odczyta
            // ja z trafionej scianki i wywola paintSquare(faceName,row,col).
            st.userData.face = faceName;
            st.userData.row = row;
            st.userData.col = col;
            const color = cube[faceName][row][col];
            if (color && COLORS[color] !== undefined) {
                st.material.color.setHex(COLORS[color]);
                st.visible = true;
            } else {
                st.visible = false;
            }
        };
        for (let r = 0; r < 3; r++)
        for (let c = 0; c < 3; c++) {
            assign(c - 1, 1, r - 1, 'up', 'up', r, c);
            assign(c - 1, -1, 1 - r, 'down', 'down', r, c);
            assign(c - 1, 1 - r, 1, 'front', 'front', r, c);
            assign(1 - c, 1 - r, -1, 'back', 'back', r, c);
            assign(1, 1 - r, 1 - c, 'right', 'right', r, c);
            assign(-1, 1 - r, c - 1, 'left', 'left', r, c);
        }
        this.render();
    }

    // Obracanie kostki - arcball w ukladzie EKRANU (pre-multiply). W trybie Paint
    // czyste klikniecie (bez przeciagniecia) maluje trafiona naklejke; przeciaganie
    // nadal obraca kostke.
    _bindPointer() {
        const el = this.renderer.domElement;
        let dragging = false, lastX = 0, lastY = 0, downX = 0, downY = 0, moved = false;
        const start = (x, y) => { dragging = true; lastX = x; lastY = y; downX = x; downY = y; moved = false; };
        const move = (x, y) => {
            if (!dragging) return;
            const dx = x - lastX, dy = y - lastY;
            lastX = x; lastY = y;
            if (Math.abs(x - downX) + Math.abs(y - downY) > 4) moved = true;
            const q = new THREE.Quaternion().setFromEuler(
                new THREE.Euler(dy * 0.008, dx * 0.008, 0, 'XYZ')
            );
            this.cubeGroup.quaternion.premultiply(q); // uklad ekranu
            this.render();
        };
        const end = (x, y) => {
            if (dragging && !moved && this.pickEnabled) this._pick(x, y);
            dragging = false;
        };

        el.addEventListener('pointerdown', e => { el.setPointerCapture(e.pointerId); start(e.clientX, e.clientY); });
        el.addEventListener('pointermove', e => move(e.clientX, e.clientY));
        el.addEventListener('pointerup', e => end(e.clientX, e.clientY));
        el.addEventListener('pointercancel', () => { dragging = false; });
    }

    // Raycasting: znajdz naklejke pod kursorem i zglos jej pozycje (face,row,col).
    // Strzelamy w ciemne korpusy (zawsze widoczne); z normalnej trafionej scianki
    // wyznaczamy kierunek, a z niego naklejke tej scianki cubie.
    _pick(clientX, clientY) {
        if (!this.onPick) return;
        const rect = this.renderer.domElement.getBoundingClientRect();
        const ndc = new THREE.Vector2(
            ((clientX - rect.left) / rect.width) * 2 - 1,
            -((clientY - rect.top) / rect.height) * 2 + 1
        );
        this.raycaster.setFromCamera(ndc, this.camera);
        const hits = this.raycaster.intersectObjects(this.bodyMeshes, false);
        if (!hits.length || !hits[0].face) return;
        const n = hits[0].face.normal; // w lokalnym ukladzie cubie (identycznym z ukladem kostki)
        const ax = Math.abs(n.x), ay = Math.abs(n.y), az = Math.abs(n.z);
        let dir;
        if (ax >= ay && ax >= az) dir = n.x > 0 ? 'right' : 'left';
        else if (ay >= ax && ay >= az) dir = n.y > 0 ? 'up' : 'down';
        else dir = n.z > 0 ? 'front' : 'back';
        const cubie = this.cubies[hits[0].object.userData.key];
        const st = cubie && cubie.stickers[dir];
        if (st && st.userData.face !== undefined) {
            this.onPick(st.userData.face, st.userData.row, st.userData.col);
        }
    }

    // Animacja obrotu warstwy (kosmetyczna). Po zakonczeniu cubie wracaja na swoje
    // miejsca, a script.js przemalowuje je do nowego stanu (setColors) - bez skoku.
    animateLayerTurn(move) {
        return new Promise(resolve => {
            const face = move.charAt(0);
            // Znak dobrany tak, by kierunek zgadzal sie z notacja (obrot danej sciany
            // "zgodnie z ruchem wskazowek" patrzac na te sciane). Sciany na dodatniej
            // osi (U,R,F) -> -1, na ujemnej (D,L,B) -> +1.
            const spec = {
                U: { axis: 'y', sign: -1 }, D: { axis: 'y', sign: 1 },
                R: { axis: 'x', sign: -1 }, L: { axis: 'x', sign: 1 },
                F: { axis: 'z', sign: -1 }, B: { axis: 'z', sign: 1 }
            }[face];
            const inLayer = {
                U: c => c.home.pos.y > 0.5, D: c => c.home.pos.y < -0.5,
                R: c => c.home.pos.x > 0.5, L: c => c.home.pos.x < -0.5,
                F: c => c.home.pos.z > 0.5, B: c => c.home.pos.z < -0.5
            }[face];
            if (!spec || !inLayer) { resolve(); return; }

            let turns = 1;
            if (move.endsWith('2')) turns = 2;
            const sign = move.endsWith("'") ? -spec.sign : spec.sign;
            const target = sign * (Math.PI / 2) * turns;

            const pivot = new THREE.Group();
            this.cubeGroup.add(pivot);
            const members = Object.values(this.cubies).filter(inLayer);
            members.forEach(c => pivot.add(c.mesh)); // pivot w origin - brak skoku

            const dur = 300 + (turns - 1) * 120;
            const t0 = performance.now();
            const ease = t => 0.5 - 0.5 * Math.cos(Math.PI * t); // ease-in-out
            const tick = now => {
                const t = Math.min(1, (now - t0) / dur);
                pivot.rotation[spec.axis] = target * ease(t);
                this.render();
                if (t < 1) { requestAnimationFrame(tick); return; }
                // koniec: cubie z powrotem do grupy, reset na pozycje bazowa
                members.forEach(c => {
                    this.cubeGroup.add(c.mesh);
                    c.mesh.position.copy(c.home.pos);
                    c.mesh.quaternion.copy(c.home.quat);
                });
                this.cubeGroup.remove(pivot);
                this.render();
                resolve();
            };
            requestAnimationFrame(tick);
        });
    }

    // Ulozono - sprezysty "pop" + delikatny rozblysk (skala grupy).
    celebrate() {
        const t0 = performance.now();
        const dur = 1100;
        const tick = now => {
            const t = Math.min(1, (now - t0) / dur);
            // 1 -> 1.12 -> 0.97 -> 1
            let s = 1;
            if (t < 0.3) s = 1 + 0.12 * (t / 0.3);
            else if (t < 0.55) s = 1.12 - 0.15 * ((t - 0.3) / 0.25);
            else s = 0.97 + 0.03 * ((t - 0.55) / 0.45);
            this.cubeGroup.scale.setScalar(s);
            this.render();
            if (t < 1) requestAnimationFrame(tick);
            else { this.cubeGroup.scale.setScalar(1); this.render(); }
        };
        requestAnimationFrame(tick);
    }

    // Dopasowanie do rozmiaru kontenera (wywolywane przy pokazaniu widoku 3D i resize).
    onShow() {
        const w = this.container.clientWidth || 320;
        const h = this.container.clientHeight || 340;
        if (w === 0 || h === 0) return;
        this.renderer.setSize(w, h, true); // updateStyle=true -> canvas CSS = w x h
        this.camera.aspect = w / h;
        this.camera.updateProjectionMatrix();
        this.render();
    }

    render() {
        this.renderer.render(this.scene, this.camera);
    }
}
