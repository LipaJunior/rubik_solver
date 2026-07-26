// Rubik's Cube Solver Frontend
class RubikCubeSolver {
    constructor() {
        this.currentCube = null;
        this.sessionId = null;
        this.solutionMoves = [];
        this.currentMoveIndex = 0;
        this.isPlayingSolution = false;
        this.isPaintMode = false;
        this.selectedColor = 'W';
        this.isScramblingPhase = false;
        // Blokada serializujaca pojedyncze ruchy - zapobiega wyscigom, gdy
        // rownolegle leci odtwarzanie i klikany jest "Next Step" (lub szybkie
        // wielokrotne klikniecia), przez co ruchy bazowaly na starym stanie.
        this.isBusy = false;
        this.viewMode = '2d';

        this.initializeCube();
        this.setupEventListeners();
        this.build3DCube();
        this.setup3DRotation();
        this.setView('2d');
        this.resetCube();
        
        // Initialize default selected color without showing status message
        this.selectColorSilently('W');

        // Stan logowania (Google)
        this.loadAuth();
    }

    // Cube initialization
    initializeCube() {
        this.currentCube = {
            up: this.createEmptyFace(),
            front: this.createEmptyFace(),
            right: this.createEmptyFace(),
            left: this.createEmptyFace(),
            back: this.createEmptyFace(),
            down: this.createEmptyFace()
        };
    }

    createEmptyFace() {
        return [
            [null, null, null],
            [null, null, null],
            [null, null, null]
        ];
    }

    // Color mapping
    getColorClass(color) {
        const colorMap = {
            'W': 'color-W',
            'R': 'color-R', 
            'B': 'color-B',
            'G': 'color-G',
            'O': 'color-O',
            'Y': 'color-Y'
        };
        return colorMap[color] || 'color-W';
    }

    // Cube rendering - wypelnia zarowno siatke 2D jak i kostke 3D z tych samych danych
    renderCube() {
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];

        // Remove solved cube class if cube is not solved
        const cubeContainer = document.querySelector('.cube-net');
        if (cubeContainer) {
            cubeContainer.classList.remove('solved-cube');
        }

        faces.forEach(faceName => {
            const el2d = document.getElementById(`${faceName}-face`);
            if (el2d) this.fillGrid(el2d, faceName, 'cube-square');
        });

        this.paint3DCubies();
    }

    // Buduje 26 "cubie" (bez srodka) jako male szescianiki. Wywolywane raz;
    // pozniej tylko przemalowujemy scianki (paint3DCubies).
    build3DCube() {
        const container = document.getElementById('cube-3d');
        if (!container) return;

        this.cubies = {};
        const S = 50; // odstep miedzy srodkami cubie

        for (let x = -1; x <= 1; x++) {
            for (let y = -1; y <= 1; y++) {
                for (let z = -1; z <= 1; z++) {
                    if (x === 0 && y === 0 && z === 0) continue; // rdzen - pomijamy

                    const cubie = document.createElement('div');
                    cubie.className = 'cubie';
                    // CSS y jest "w dol", nasze y jest "w gore" -> minus.
                    cubie.style.transform = `translate3d(${x * S}px, ${-y * S}px, ${z * S}px)`;

                    const faces = {};
                    ['up', 'down', 'front', 'back', 'right', 'left'].forEach(dir => {
                        const f = document.createElement('div');
                        f.className = 'cubie-face cf-' + dir;
                        cubie.appendChild(f);
                        faces[dir] = f;
                    });

                    container.appendChild(cubie);
                    this.cubies[`${x},${y},${z}`] = { el: cubie, x, y, z, faces };
                }
            }
        }
    }

    // Koloruje zewnetrzne scianki cubie na podstawie currentCube. Mapowanie
    // (sciana,row,col) -> (x,y,z,kierunek) wyprowadzone z geometrii plaskich scian.
    paint3DCubies() {
        if (!this.cubies || !this.currentCube) return;

        const cube = this.currentCube;
        const assign = (x, y, z, dir, faceName, row, col) => {
            const c = this.cubies[`${x},${y},${z}`];
            if (!c) return;
            const f = c.faces[dir];
            f.className = 'cubie-face cf-' + dir;
            const color = cube[faceName][row][col];
            if (color) f.classList.add(this.getColorClass(color));
            f.onclick = this.isPaintMode ? () => this.paintSquare(faceName, row, col) : null;
        };

        for (let r = 0; r < 3; r++) {
            for (let c = 0; c < 3; c++) {
                assign(c - 1, 1, r - 1, 'up', 'up', r, c);
                assign(c - 1, -1, 1 - r, 'down', 'down', r, c);
                assign(c - 1, 1 - r, 1, 'front', 'front', r, c);
                assign(1 - c, 1 - r, -1, 'back', 'back', r, c);
                assign(1, 1 - r, 1 - c, 'right', 'right', r, c);
                assign(-1, 1 - r, c - 1, 'left', 'left', r, c);
            }
        }
    }

    // Wypelnia pojedyncza siatke 3x3 naklejkami danej sciany
    fillGrid(gridElement, faceName, squareClass) {
        gridElement.innerHTML = '';

        for (let row = 0; row < 3; row++) {
            for (let col = 0; col < 3; col++) {
                const square = document.createElement('div');
                square.className = squareClass;

                // Add attributes for position identification
                square.dataset.face = faceName;
                square.dataset.row = row;
                square.dataset.col = col;

                const color = this.currentCube[faceName][row][col];
                if (color) {
                    square.classList.add(this.getColorClass(color));
                }

                // Add click handling in paint mode
                if (this.isPaintMode) {
                    square.addEventListener('click', () => {
                        this.paintSquare(faceName, row, col);
                    });
                }

                gridElement.appendChild(square);
            }
        }
    }

    // Przelaczanie widoku 2D / 3D
    setView(mode) {
        this.viewMode = mode;
        document.querySelector('.cube-net').style.display = mode === '2d' ? 'grid' : 'none';
        document.querySelector('.cube-3d-scene').style.display = mode === '3d' ? 'flex' : 'none';
        document.getElementById('view-2d-btn').classList.toggle('active', mode === '2d');
        document.getElementById('view-3d-btn').classList.toggle('active', mode === '3d');
    }

    // Obracanie kostki 3D - mysz (desktop) i dotyk (telefon/tablet)
    setup3DRotation() {
        const scene = document.querySelector('.cube-3d-scene');
        const cube = document.querySelector('.cube-3d');
        if (!scene || !cube) return;

        this.rotX = -30;
        this.rotY = -45;
        let dragging = false;
        let lastX = 0;
        let lastY = 0;

        const apply = () => {
            // Ustawiamy kat przez zmienne CSS, a nie inline transform - dzieki temu
            // animacja "spin" po ulozeniu moze uzyc tych samych zmiennych.
            cube.style.setProperty('--rx', this.rotX + 'deg');
            cube.style.setProperty('--ry', this.rotY + 'deg');
        };
        apply();

        const start = (x, y) => { dragging = true; lastX = x; lastY = y; };
        const move = (x, y) => {
            if (!dragging) return;
            this.rotY += (x - lastX) * 0.5;
            this.rotX -= (y - lastY) * 0.5;
            lastX = x;
            lastY = y;
            apply();
        };
        const end = () => { dragging = false; };

        // Mysz
        scene.addEventListener('mousedown', (e) => start(e.clientX, e.clientY));
        window.addEventListener('mousemove', (e) => move(e.clientX, e.clientY));
        window.addEventListener('mouseup', end);

        // Dotyk
        scene.addEventListener('touchstart', (e) => {
            const t = e.touches[0];
            start(t.clientX, t.clientY);
        }, { passive: true });
        scene.addEventListener('touchmove', (e) => {
            if (!dragging) return;
            e.preventDefault(); // nie przewijaj strony podczas obracania kostki
            const t = e.touches[0];
            move(t.clientX, t.clientY);
        }, { passive: false });
        window.addEventListener('touchend', end);
        window.addEventListener('touchcancel', end);
    }

    // API communication
    async makeApiCall(endpoint, method = 'POST', data = null) {
        try {
            const options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                }
            };

            if (data) {
                options.body = JSON.stringify(data);
            }

            let response;
            if (this.sessionId) {
                const url = new URL(`/cube/${endpoint}`, window.location.origin);
                url.searchParams.append('sessionId', this.sessionId);
                response = await fetch(url, options);
            } else {
                response = await fetch(`/cube/${endpoint}`, options);
            }

            // Check if response is ok
            if (!response.ok) {
                return {
                    error: true,
                    status: response.status,
                    statusText: response.statusText
                };
            }

            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            this.showStatus('Connection error with server', 'error');
            return null;
        }
    }

    // Move execution
    async makeMove(move) {
        if (!this.currentCube) return;

        this.showStatus(`Executing move: ${move}`, 'info');
        
        const response = await this.makeApiCall('move', 'POST', {
            cube: this.currentCube,
            moves: [move]
        });

        if (response && response.cube) {
            // W widoku 3D pokaz obrot warstwy zanim "dociagniemy" do nowego stanu.
            if (this.viewMode === '3d' && this.cubies) {
                await this.animateLayerTurn(move);
            }
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.showStatus(`Move ${move} executed`, 'success');
        } else {
            this.showStatus(`Error executing move: ${move}`, 'error');
        }
    }

    // Animacja obrotu warstwy w 3D: 9 cubie danej warstwy trafia do "pivota"
    // obracanego wokol srodka kostki o 90 (lub 180) stopni. Po czasie animacji
    // cubie wracaja, a makeMove przemalowuje je do docelowego stanu (bez skoku).
    animateLayerTurn(move) {
        return new Promise(resolve => {
            const container = document.getElementById('cube-3d');
            const face = move.charAt(0);

            // Os obrotu (CSS) i znak dla obrotu "zgodnie z ruchem wskazowek" danej sciany.
            const spec = {
                U: { axis: 'Y', deg: -90 }, D: { axis: 'Y', deg: 90 },
                R: { axis: 'X', deg: 90 },  L: { axis: 'X', deg: -90 },
                F: { axis: 'Z', deg: 90 },  B: { axis: 'Z', deg: -90 }
            }[face];
            const inLayer = {
                U: c => c.y === 1, D: c => c.y === -1,
                R: c => c.x === 1, L: c => c.x === -1,
                F: c => c.z === 1, B: c => c.z === -1
            }[face];

            if (!container || !spec || !inLayer) { resolve(); return; }

            let deg = spec.deg;
            if (move.endsWith("'")) deg = -deg;
            if (move.endsWith('2')) deg = 180;

            const layerCubies = Object.values(this.cubies).filter(inLayer);
            const pivot = document.createElement('div');
            pivot.className = 'layer-pivot';
            container.appendChild(pivot);
            layerCubies.forEach(c => pivot.appendChild(c.el));

            // wymus reflow, potem odpal przejscie
            void pivot.offsetWidth;
            pivot.style.transition = 'transform 0.28s ease-in-out';
            pivot.style.transform = `rotate${spec.axis}(${deg}deg)`;

            // Czekamy na czas trwania (setTimeout dziala tez, gdy transitionend
            // nie odpali - np. karta w tle). Potem przywracamy cubie.
            setTimeout(() => {
                layerCubies.forEach(c => container.appendChild(c.el));
                pivot.remove();
                resolve();
            }, 300);
        });
    }

    // Reczny ruch (przyciski/klawiatura) - serializowany ta sama blokada co Next Step,
    // zeby szybkie klikniecia nie nadpisywaly sie nawzajem, i zablokowany w trakcie
    // odtwarzania rozwiazania.
    async doManualMove(move) {
        if (this.isPlayingSolution || this.isBusy) return;

        this.isBusy = true;
        try {
            await this.makeMove(move);
        } finally {
            this.isBusy = false;
        }
    }

    // Start scrambling phase
    startScramblingPhase() {
        this.isScramblingPhase = true;
        
        // Show cube moves section
        document.getElementById('move-controls').style.display = 'block';
        
        // Hide solve button
        document.getElementById('solve-btn').style.display = 'none';
        
        // Change scramble button text
        document.getElementById('scramble-btn').textContent = '🔄 New Cube';
        
        // Clear solution
        this.solutionMoves = [];
        this.currentMoveIndex = 0;
        this.updateSolutionDisplay();
        
        this.showStatus('Now you can scramble the cube using the move buttons. Click "Done" when finished!', 'info');
    }

    // Finish scrambling phase and move to solving
    finishScramblingPhase() {
        this.isScramblingPhase = false;
        
        // Hide cube moves section
        document.getElementById('move-controls').style.display = 'none';
        
        // Show solve button
        document.getElementById('solve-btn').style.display = 'inline-block';
        
        // Change scramble button text back
        document.getElementById('scramble-btn').textContent = '🔀 Scramble';
        
        this.showStatus('Scrambling finished!', 'success');
    }

    // Cube scrambling (automatic mixing)
    async scrambleCube() {
        this.showStatus('Scrambling cube...', 'info');
        
        const response = await this.makeApiCall('scramble', 'POST', {
            cube: this.currentCube,
            n: 20
        });

        if (response && response.cube) {
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.solutionMoves = [];
            this.currentMoveIndex = 0;
            this.updateSolutionDisplay();
            this.showStatus('Cube scrambled!', 'success');
        } else {
            this.showStatus('Error scrambling cube', 'error');
        }
    }

    // Random cube
    async loadRandomCube() {
        this.showStatus('Loading random cube...', 'info');
        
        const response = await this.makeApiCall('random', 'POST');

        if (response && response.cube) {
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.solutionMoves = [];
            this.currentMoveIndex = 0;
            this.updateSolutionDisplay();
            
            if (this.isScramblingPhase) {
                this.showStatus('New cube loaded! Continue scrambling.', 'success');
            } else {
                this.showStatus('Random cube loaded!', 'success');
            }
        } else {
            this.showStatus('Error loading cube', 'error');
        }
    }

    // Reset kostki - pokazuje rozwiązana kostkę bez efektów
    async resetCube() {
        this.showStatus('Loading solved cube...', 'info');
        
        // Reset interface state
        this.isScramblingPhase = false;
        this.isPaintMode = false;
        
        // Ukryj sekcję ruchów kostki
        document.getElementById('move-controls').style.display = 'none';
        
        // Pokaż przycisk solve
        document.getElementById('solve-btn').style.display = 'inline-block';
        
        // Zmień tekst przycisku scramble z powrotem
        document.getElementById('scramble-btn').textContent = '🔀 Scramble';
        
        // Wyłącz tryb malowania jeśli był włączony
        const paintBtn = document.getElementById('paint-mode-btn');
        const colorPicker = document.getElementById('color-picker');
        const paintInfo = document.getElementById('paint-info');
        const cubeContainer = document.querySelector('.cube-net');
        
        paintBtn.textContent = '🎨 Paint the cube';
        paintBtn.classList.remove('active');
        colorPicker.style.display = 'none';
        paintInfo.style.display = 'none';
        cubeContainer.classList.remove('paint-mode-active');
        
        // Utwórz rozwiązana kostkę na froncie
        this.currentCube = {
            up: this.createCompletedFace('W'),
            front: this.createCompletedFace('R'),
            right: this.createCompletedFace('B'),
            left: this.createCompletedFace('G'),
            back: this.createCompletedFace('O'),
            down: this.createCompletedFace('Y')
        };
        
        this.renderCube();
        this.solutionMoves = [];
        this.currentMoveIndex = 0;
        this.updateSolutionDisplay();
        
        this.showStatus('Solved cube loaded!', 'success');
    }

    // Helper method to create solved face
    createCompletedFace(color) {
        return [
            [color, color, color],
            [color, color, color],
            [color, color, color]
        ];
    }

    // Painting square
    paintSquare(faceName, row, col) {
        if (!this.isPaintMode) return;
        
        // Update color in cube structure
        this.currentCube[faceName][row][col] = this.selectedColor;
        
        // Re-render cube
        this.renderCube();
        
        // Clear solution (cube has changed)
        this.solutionMoves = [];
        this.currentMoveIndex = 0;
        this.updateSolutionDisplay();
        
        this.showStatus(`Painted square (${faceName}, ${row}, ${col}) with color ${this.selectedColor}`, 'info');
    }

    // Toggle paint mode
    togglePaintMode() {
        this.isPaintMode = !this.isPaintMode;
        const paintBtn = document.getElementById('paint-mode-btn');
        const colorPicker = document.getElementById('color-picker');
        const paintInfo = document.getElementById('paint-info');
        const cubeContainer = document.querySelector('.cube-net');
        
        if (this.isPaintMode) {
            paintBtn.textContent = '🚫 Finish painting';
            paintBtn.classList.add('active');
            colorPicker.style.display = 'grid';
            paintInfo.style.display = 'block';
            cubeContainer.classList.add('paint-mode-active');
            this.showStatus('Paint mode enabled - click on cube squares', 'info');
        } else {
            paintBtn.textContent = '🎨 Paint the cube';
            paintBtn.classList.remove('active');
            colorPicker.style.display = 'none';
            paintInfo.style.display = 'none';
            cubeContainer.classList.remove('paint-mode-active');
            this.showStatus('Paint mode disabled', 'info');
        }
        
        // Re-render cube with new event listeners
        this.renderCube();
    }

    // Color selection
    selectColor(color) {
        this.selectedColor = color;
        
        // Update selected color display
        document.querySelectorAll('.color-option').forEach(option => {
            option.classList.remove('selected');
        });
        
        document.querySelector(`[data-color="${color}"]`).classList.add('selected');
        document.getElementById('selected-color').textContent = color;
        
        this.showStatus(`Selected color: ${color}`, 'info');
    }

    // Silent color selection (without status message)
    selectColorSilently(color) {
        this.selectedColor = color;
        
        // Update selected color display
        document.querySelectorAll('.color-option').forEach(option => {
            option.classList.remove('selected');
        });
        
        document.querySelector(`[data-color="${color}"]`).classList.add('selected');
        document.getElementById('selected-color').textContent = color;
    }

    // Solved cube animation
    animateSolvedCube() {
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        
        faces.forEach((faceName, index) => {
            const faceElement = document.getElementById(`${faceName}-face`);
            if (faceElement) {
                // Add animation class with delay
                setTimeout(() => {
                    faceElement.classList.add('face-rotating');
                    
                    // Remove class after animation ends
                    setTimeout(() => {
                        faceElement.classList.remove('face-rotating');
                    }, 500);
                }, index * 100);
            }
        });
    }

    // Check if cube is solved
    isCubeSolved() {
        if (!this.currentCube) return false;
        
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        const expectedColors = ['W', 'R', 'B', 'G', 'O', 'Y'];
        
        for (let i = 0; i < faces.length; i++) {
            const face = this.currentCube[faces[i]];
            const expectedColor = expectedColors[i];
            
            // Check if entire face has the same color
            for (let row = 0; row < 3; row++) {
                for (let col = 0; col < 3; col++) {
                    if (face[row][col] !== expectedColor) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    // Show solved cube effects - zaleznie od aktywnego widoku (2D albo 3D),
    // zeby nie odpalac animacji 2D na ukrytej siatce w trybie 3D.
    showSolvedEffects() {
        if (this.viewMode === '3d') {
            this.celebrate3D();
        } else {
            document.querySelector('.cube-net').classList.add('solved-cube');
            this.animateSolvedCube();
        }

        this.showStatus('Congratulations! Cube has been solved!', 'success');
    }

    // Animacja ulozenia dla kostki 3D: pelny obrot kostki o 360 stopni + zielona
    // poswiata. Obrot uzywa zmiennych --rx/--ry (patrz setup3DRotation), a poswiata
    // jest na scenie - dzieki temu nie koliduja ze soba. Animacje CSS same sie koncza.
    celebrate3D() {
        const scene = document.querySelector('.cube-3d-scene');
        const cube = document.querySelector('.cube-3d');
        if (!cube) return;

        cube.classList.add('solved-spin');
        if (scene) scene.classList.add('solved-glow');

        setTimeout(() => {
            cube.classList.remove('solved-spin');
            if (scene) scene.classList.remove('solved-glow');
        }, 1100);
    }

    // Cube solving
    async solveCube() {
        if (!this.currentCube) return;

        this.showStatus('Solving cube...', 'info');
        
        const response = await this.makeApiCall('solve', 'POST', {
            cube: this.currentCube
        });

        // Check if response contains an error
        if (response && response.error) {
            if (response.status === 400) {
                this.showStatus('❌ Illegal Cube!', 'error');
            } else {
                this.showStatus(`Failed to solve cube (${response.status})`, 'error');
            }
            return;
        }

        if (response && response.moves) {
            this.solutionMoves = response.moves;
            this.currentMoveIndex = 0;
            this.sessionId = response.sessionId;
            this.updateSolutionDisplay();
            this.showStatus(`Solution found: ${this.solutionMoves.length} moves`, 'success');
        } else {
            this.showStatus('Failed to solve cube', 'error');
        }
    }

    // Solution display update
    updateSolutionDisplay() {
        const container = document.getElementById('solution-moves');
        const playBtn = document.getElementById('play-solution-btn');
        const stepBtn = document.getElementById('step-solution-btn');

        container.innerHTML = '';

        if (this.solutionMoves.length === 0) {
            container.innerHTML = '<span style="color: #6c757d;">No solution</span>';
            playBtn.disabled = true;
            stepBtn.disabled = true;
            return;
        }

        this.solutionMoves.forEach((move, index) => {
            const moveElement = document.createElement('span');
            moveElement.className = 'solution-move';
            moveElement.textContent = move;
            
            if (index < this.currentMoveIndex) {
                moveElement.classList.add('completed');
            } else if (index === this.currentMoveIndex) {
                moveElement.classList.add('current');
            }
            
            container.appendChild(moveElement);
        });

        playBtn.disabled = false;
        // Krokowanie zablokowane w trakcie odtwarzania (patrz playSolution).
        stepBtn.disabled = this.isPlayingSolution;
    }

    // Solution playback
    async playSolution() {
        // Nie startuj, gdy juz gra, gdy trwa pojedynczy ruch (Next Step), albo gdy
        // rozwiazanie jest juz odtworzone do konca.
        if (this.isPlayingSolution || this.isBusy
                || this.currentMoveIndex >= this.solutionMoves.length) {
            return;
        }

        this.isPlayingSolution = true;
        const playBtn = document.getElementById('play-solution-btn');
        const stepBtn = document.getElementById('step-solution-btn');
        playBtn.textContent = '⏸️ Pause';
        stepBtn.disabled = true; // brak recznego krokowania w trakcie odtwarzania

        while (this.currentMoveIndex < this.solutionMoves.length && this.isPlayingSolution) {
            const move = this.solutionMoves[this.currentMoveIndex];
            await this.makeMove(move);
            this.currentMoveIndex++;
            this.updateSolutionDisplay();

            // Short pause between moves
            await new Promise(resolve => setTimeout(resolve, 500));
        }

        this.isPlayingSolution = false;
        playBtn.textContent = '▶️ Play Solution';
        stepBtn.disabled = this.currentMoveIndex >= this.solutionMoves.length;

        if (this.currentMoveIndex >= this.solutionMoves.length) {
            // Check if cube is solved and add effects
            if (this.isCubeSolved()) {
                this.showSolvedEffects();
            }
            this.showStatus('Solution completed!', 'success');
        }
    }

    // Solution pausing
    pauseSolution() {
        this.isPlayingSolution = false;
        const playBtn = document.getElementById('play-solution-btn');
        const stepBtn = document.getElementById('step-solution-btn');
        playBtn.textContent = '▶️ Play Solution';
        stepBtn.disabled = this.currentMoveIndex >= this.solutionMoves.length;
        this.showStatus('Solution paused', 'info');
    }

    // Next solution step
    async nextStep() {
        // Nie krokuj w trakcie odtwarzania ani gdy inny ruch wlasnie trwa.
        if (this.isPlayingSolution || this.isBusy) {
            return;
        }
        if (this.currentMoveIndex >= this.solutionMoves.length) {
            // Juz na koncu - efekt odpalil sie przy ostatnim ruchu, nic nie rob.
            return;
        }

        this.isBusy = true;
        try {
            const move = this.solutionMoves[this.currentMoveIndex];
            await this.makeMove(move);
            this.currentMoveIndex++;
            this.updateSolutionDisplay();

            // Jesli to byl ostatni ruch - pokaz efekt od razu (bez dodatkowego klikniecia).
            if (this.currentMoveIndex >= this.solutionMoves.length) {
                this.showStatus('Solution completed!', 'success');
                if (this.isCubeSolved()) {
                    this.showSolvedEffects();
                }
            }
        } finally {
            this.isBusy = false;
        }
    }

    // Status display
    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('status-message');

        statusElement.textContent = message;
        statusElement.className = `status-message ${type}`;
    }

    // Logowanie (Google) - pobiera status i renderuje login/logout w naglowku.
    async loadAuth() {
        try {
            const res = await fetch('/api/me');
            if (!res.ok) return;
            const data = await res.json();
            this.isPremium = !!data.premium;
            this.renderAuth(data);
        } catch (e) {
            // brak endpointu (np. lokalnie bez OAuth) - po prostu nic nie pokazuj
        }
    }

    renderAuth(data) {
        const el = document.getElementById('auth-area');
        if (!el) return;
        el.innerHTML = '';

        if (data.authenticated) {
            const span = document.createElement('span');
            span.textContent = '👤 ' + (data.name || data.email) + (data.premium ? ' ⭐ Premium' : '');
            el.appendChild(span);

            if (!data.premium) {
                const upgrade = document.createElement('button');
                upgrade.textContent = '⭐ Upgrade';
                upgrade.addEventListener('click', () => this.upgrade());
                el.appendChild(upgrade);
            }

            const btn = document.createElement('button');
            btn.textContent = 'Wyloguj';
            btn.addEventListener('click', () => this.logout());
            el.appendChild(btn);
        } else {
            const a = document.createElement('a');
            a.href = '/oauth2/authorization/google';
            a.textContent = 'Zaloguj przez Google';
            el.appendChild(a);
        }
    }

    async logout() {
        try { await fetch('/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
        window.location.reload();
    }

    // Rozpoczyna platnosc: backend tworzy sesje Stripe, przekierowujemy na jej URL.
    async upgrade() {
        try {
            const res = await fetch('/api/checkout', { method: 'POST' });
            if (!res.ok) {
                this.showStatus('Nie udalo sie rozpoczac platnosci', 'error');
                return;
            }
            const data = await res.json();
            window.location = data.url; // strona platnosci Stripe
        } catch (e) {
            this.showStatus('Nie udalo sie rozpoczac platnosci', 'error');
        }
    }

    // Event listeners configuration
    setupEventListeners() {
        // Move buttons
        document.querySelectorAll('.move-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const move = e.target.dataset.move;
                this.doManualMove(move);
            });
        });

        // Scramble button
        document.getElementById('scramble-btn').addEventListener('click', () => {
            if (this.isScramblingPhase) {
                // If we're in scrambling phase, load new cube
                this.loadRandomCube();
            } else {
                // Start scrambling phase
                this.startScramblingPhase();
            }
        });

        // Solve button
        document.getElementById('solve-btn').addEventListener('click', () => {
            this.solveCube();
        });

        // "Done" button - finish scrambling phase
        document.getElementById('done-scrambling-btn').addEventListener('click', () => {
            this.finishScramblingPhase();
        });

        // Reset button
        document.getElementById('reset-btn').addEventListener('click', () => {
            this.resetCube();
        });

        // Random button
        document.getElementById('random-btn').addEventListener('click', () => {
            this.loadRandomCube();
        });

        // View toggle 2D / 3D
        document.getElementById('view-2d-btn').addEventListener('click', () => {
            this.setView('2d');
        });
        document.getElementById('view-3d-btn').addEventListener('click', () => {
            this.setView('3d');
        });

        // Play solution button (toggluje play/pauza - jeden handler, bez onclick)
        document.getElementById('play-solution-btn').addEventListener('click', () => {
            if (this.isPlayingSolution) {
                this.pauseSolution();
            } else {
                this.playSolution();
            }
        });

        // Next step button
        document.getElementById('step-solution-btn').addEventListener('click', () => {
            this.nextStep();
        });

        // Paint mode button
        document.getElementById('paint-mode-btn').addEventListener('click', () => {
            this.togglePaintMode();
        });

        // Color selection
        document.querySelectorAll('.color-option').forEach(option => {
            option.addEventListener('click', (e) => {
                const color = e.target.dataset.color;
                this.selectColor(color);
            });
        });

        // Keyboard handling
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) return; // Ignore keyboard shortcuts
            
            const keyMap = {
                'r': 'R',
                'R': 'R\'',
                'l': 'L', 
                'L': 'L\'',
                'u': 'U',
                'U': 'U\'',
                'd': 'D',
                'D': 'D\'',
                'f': 'F',
                'F': 'F\'',
                'b': 'B',
                'B': 'B\''
            };

            const move = keyMap[e.key];
            if (move) {
                e.preventDefault();
                this.doManualMove(move);
            }
        });
    }
}

// Application initialization after DOM load
document.addEventListener('DOMContentLoaded', () => {
    new RubikCubeSolver();
});

// Additional styles for status
const style = document.createElement('style');
style.textContent = `
    .status-message.success { color: #28a745; }
    .status-message.error { color: #dc3545; }
    .status-message.info { color: #17a2b8; }
`;
document.head.appendChild(style);
