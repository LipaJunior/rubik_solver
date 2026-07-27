// Rubik's Cube Solver Frontend
import { Cube3D } from './cube3d.js';

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
    // Inicjalizacja sceny 3D (Three.js/WebGL). Sam render jest w cube3d.js.
    build3DCube() {
        const container = document.getElementById('cube-3d-scene');
        if (!container) return;
        try {
            this.cube3d = new Cube3D(container);
            // Malowanie klikaniem po kostce 3D (w trybie Paint).
            this.cube3d.onPick = (faceName, row, col) => this.paintSquare(faceName, row, col);
            this.cube3d.pickEnabled = this.isPaintMode;
        } catch (e) {
            console.error('WebGL 3D init failed:', e);
            this.cube3d = null;
        }
    }

    // Przekazuje aktualny stan kostki do sceny 3D (koloruje naklejki).
    paint3DCubies() {
        if (!this.cube3d || !this.currentCube) return;
        this.cube3d.setColors(this.currentCube);
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
        document.querySelector('.cube-3d-scene').style.display = mode === '3d' ? 'block' : 'none';
        document.getElementById('view-2d-btn').classList.toggle('active', mode === '2d');
        document.getElementById('view-3d-btn').classList.toggle('active', mode === '3d');
        // Canvas WebGL musi sie przemierzyc/przerenderowac, gdy staje sie widoczny.
        if (mode === '3d' && this.cube3d) this.cube3d.onShow();
    }

    // Obracanie kostki 3D jest teraz obslugiwane wewnatrz Cube3D (Three.js) - arcball
    // na canvasie. Metoda zostaje jako no-op dla zgodnosci z konstruktorem.
    setup3DRotation() {}

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
            if (this.viewMode === '3d' && this.cube3d) {
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

    // Animacja obrotu warstwy w 3D - delegowana do Cube3D (Three.js).
    animateLayerTurn(move) {
        if (!this.cube3d) return Promise.resolve();
        return this.cube3d.animateLayerTurn(move);
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
        if (this.cube3d) this.cube3d.pickEnabled = false;

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
        if (this.cube3d) this.cube3d.pickEnabled = this.isPaintMode; // malowanie w 3D
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

    // Animacja ulozenia dla kostki 3D: sprezysty "pop" + zielona poswiata na SCENIE.
    // Nie ruszamy transformacji kostki (ta jest macierza z obracania), wiec nie ma
    // konfliktu. Czysto CSS, samo sie konczy.
    celebrate3D() {
        if (this.cube3d) this.cube3d.celebrate();
    }

    // Cube solving
    async solveCube() {
        if (!this.currentCube) return;

        // Widok "etapami" wlaczony -> uzyj sciezki premium. Premium egzekwuje SERWER
        // (403 jesli brak), wiec nie polegamy na fladze klienta.
        const stagedToggle = document.getElementById('staged-toggle');
        if (stagedToggle && stagedToggle.checked) {
            return this.solveCubeStaged();
        }

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
            this.solutionStages = null;
            this.solutionMoves = response.moves;
            this.currentMoveIndex = 0;
            this.sessionId = response.sessionId;
            this.updateSolutionDisplay();
            this.showStatus(`Solution found: ${this.solutionMoves.length} moves`, 'success');
        } else {
            this.showStatus('Failed to solve cube', 'error');
        }
    }

    // Premium: rozwiazanie "krok po kroku" (etapami). Kazdy etap zoptymalizowany
    // osobno; do odtwarzania sklejamy wszystkie ruchy w jedna liste.
    async solveCubeStaged() {
        this.showStatus('Solving (etapami)...', 'info');
        try {
            const res = await fetch('/api/solve-stages', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ cube: this.currentCube })
            });
            if (res.status === 403) {
                this.showStatus('Widok etapami jest funkcja premium', 'error');
                return;
            }
            if (!res.ok) {
                this.showStatus('Nie udalo sie rozwiazac kostki', 'error');
                return;
            }
            const stages = await res.json();
            this.solutionStages = stages;
            this.solutionMoves = stages.flatMap(s => s.moves);
            this.currentMoveIndex = 0;
            this.updateSolutionDisplay();
            this.showStatus(`Rozwiazanie w ${stages.length} etapach (${this.solutionMoves.length} ruchow)`, 'success');
        } catch (e) {
            this.showStatus('Nie udalo sie rozwiazac kostki', 'error');
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
            this.solutionStages = null;
            playBtn.disabled = true;
            stepBtn.disabled = true;
            return;
        }

        // Kolorowanie pojedynczego ruchu wg globalnego indeksu odtwarzania.
        const styleMove = (span, globalIndex) => {
            if (globalIndex < this.currentMoveIndex) span.classList.add('completed');
            else if (globalIndex === this.currentMoveIndex) span.classList.add('current');
        };

        if (this.solutionStages) {
            // Widok etapami (premium).
            let globalIndex = 0;
            this.solutionStages.forEach(stage => {
                const header = document.createElement('div');
                header.className = 'stage-header';
                header.textContent = `${stage.name} (${stage.moves.length})`;
                if (stage.description) header.title = stage.description;
                container.appendChild(header);

                const row = document.createElement('div');
                row.className = 'stage-moves';
                stage.moves.forEach(move => {
                    const span = document.createElement('span');
                    span.className = 'solution-move';
                    span.textContent = move;
                    styleMove(span, globalIndex);
                    row.appendChild(span);
                    globalIndex++;
                });
                container.appendChild(row);
            });
        } else {
            // Widok plaski (zoptymalizowana calosc).
            this.solutionMoves.forEach((move, index) => {
                const span = document.createElement('span');
                span.className = 'solution-move';
                span.textContent = move;
                styleMove(span, index);
                container.appendChild(span);
            });
        }

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

        // Przelacznik "etapami" widoczny tylko dla premium.
        const stagedLabel = document.getElementById('staged-toggle-label');
        if (stagedLabel) stagedLabel.style.display = data.premium ? 'flex' : 'none';

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

        // Przelacznik "etapami": jesli jest juz pokazane rozwiazanie, od razu przelicz
        // w nowym trybie (zeby zmiana switcha natychmiast zmieniala widok).
        const stagedToggle = document.getElementById('staged-toggle');
        if (stagedToggle) {
            stagedToggle.addEventListener('change', () => {
                if (this.currentCube && this.solutionMoves && this.solutionMoves.length > 0) {
                    this.solveCube();
                }
            });
        }

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
