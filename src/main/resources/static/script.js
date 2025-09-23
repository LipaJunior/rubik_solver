// Rubik's Cube Solver Frontend
class RubikCubeSolver {
    constructor() {
        this.currentCube = null;
        this.sessionId = null;
        this.solutionMoves = [];
        this.currentMoveIndex = 0;
        this.isPlayingSolution = false;
        
        this.initializeCube();
        this.setupEventListeners();
        this.loadRandomCube();
    }

    // Inicjalizacja kostki
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

    // Mapowanie kolorów
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

    // Renderowanie kostki
    renderCube() {
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        
        // Usuń klasę rozwiązanej kostki jeśli kostka nie jest rozwiązana
        const cubeContainer = document.querySelector('.cube-net');
        if (cubeContainer) {
            cubeContainer.classList.remove('solved-cube');
        }
        
        faces.forEach(faceName => {
            const faceElement = document.getElementById(`${faceName}-face`);
            if (!faceElement) return;

            faceElement.innerHTML = '';
            
            for (let row = 0; row < 3; row++) {
                for (let col = 0; col < 3; col++) {
                    const square = document.createElement('div');
                    square.className = 'cube-square';
                    
                    const color = this.currentCube[faceName][row][col];
                    if (color) {
                        square.classList.add(this.getColorClass(color));
                    }
                    
                    faceElement.appendChild(square);
                }
            }
        });
    }

    // Komunikacja z API
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

            if (this.sessionId) {
                const url = new URL(`/cube/${endpoint}`, window.location.origin);
                url.searchParams.append('sessionId', this.sessionId);
                const response = await fetch(url, options);
                return await response.json();
            } else {
                const response = await fetch(`/cube/${endpoint}`, options);
                return await response.json();
            }
        } catch (error) {
            console.error('API Error:', error);
            this.showStatus('Błąd połączenia z serwerem', 'error');
            return null;
        }
    }

    // Wykonanie ruchu
    async makeMove(move) {
        if (!this.currentCube) return;

        this.showStatus(`Wykonywanie ruchu: ${move}`, 'info');
        
        const response = await this.makeApiCall('move', 'POST', {
            cube: this.currentCube,
            moves: [move]
        });

        if (response && response.cube) {
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.showStatus(`Ruch ${move} wykonany`, 'success');
        } else {
            this.showStatus(`Błąd wykonania ruchu: ${move}`, 'error');
        }
    }

    // Scramble kostki
    async scrambleCube() {
        this.showStatus('Mieszanie kostki...', 'info');
        
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
            this.showStatus('Kostka wymieszana!', 'success');
        } else {
            this.showStatus('Błąd mieszania kostki', 'error');
        }
    }

    // Losowa kostka
    async loadRandomCube() {
        this.showStatus('Ładowanie losowej kostki...', 'info');
        
        const response = await this.makeApiCall('random', 'POST');

        if (response && response.cube) {
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.solutionMoves = [];
            this.currentMoveIndex = 0;
            this.updateSolutionDisplay();
            this.showStatus('Losowa kostka załadowana!', 'success');
        } else {
            this.showStatus('Błąd ładowania kostki', 'error');
        }
    }

    // Reset kostki - pokazuje rozwiązana kostkę bez efektów
    async resetCube() {
        this.showStatus('Ładowanie rozwiązanej kostki...', 'info');
        
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
        
        this.showStatus('Rozwiązana kostka załadowana!', 'success');
    }

    // Pomocnicza metoda do tworzenia rozwiązanej ścianki
    createCompletedFace(color) {
        return [
            [color, color, color],
            [color, color, color],
            [color, color, color]
        ];
    }

    // Animacja rozwiązanej kostki
    animateSolvedCube() {
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        
        faces.forEach((faceName, index) => {
            const faceElement = document.getElementById(`${faceName}-face`);
            if (faceElement) {
                // Dodaj klasę animacji z opóźnieniem
                setTimeout(() => {
                    faceElement.classList.add('face-rotating');
                    
                    // Usuń klasę po zakończeniu animacji
                    setTimeout(() => {
                        faceElement.classList.remove('face-rotating');
                    }, 500);
                }, index * 100);
            }
        });
    }

    // Sprawdź czy kostka jest rozwiązana
    isCubeSolved() {
        if (!this.currentCube) return false;
        
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        const expectedColors = ['W', 'R', 'B', 'G', 'O', 'Y'];
        
        for (let i = 0; i < faces.length; i++) {
            const face = this.currentCube[faces[i]];
            const expectedColor = expectedColors[i];
            
            // Sprawdź czy cała ścianka ma ten sam kolor
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

    // Pokaż efekty rozwiązanej kostki
    showSolvedEffects() {
        // Dodaj klasę rozwiązanej kostki
        const cubeContainer = document.querySelector('.cube-net');
        cubeContainer.classList.add('solved-cube');
        
        // Dodaj animację rozwiązanej kostki
        this.animateSolvedCube();
        
        // Dodaj efekt wizualny sukcesu
        this.showSuccessEffect();
        
        this.showStatus('🎉 Gratulacje! Kostka została rozwiązana!', 'success');
    }

    // Efekt wizualny sukcesu
    showSuccessEffect() {
        // Dodaj konfetti effect (prosty)
        const container = document.querySelector('.container');
        const confetti = document.createElement('div');
        confetti.innerHTML = '🎉✨🎊✨🎉';
        confetti.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 3rem;
            z-index: 1000;
            pointer-events: none;
            animation: confetti 2s ease-out forwards;
        `;
        
        container.appendChild(confetti);
        
        // Usuń po animacji
        setTimeout(() => {
            if (confetti.parentNode) {
                confetti.parentNode.removeChild(confetti);
            }
        }, 2000);
    }

    // Rozwiązywanie kostki
    async solveCube() {
        if (!this.currentCube) return;

        this.showStatus('Rozwiązywanie kostki...', 'info');
        
        const response = await this.makeApiCall('solve', 'POST', {
            cube: this.currentCube
        });

        if (response && response.moves) {
            this.solutionMoves = response.moves;
            this.currentMoveIndex = 0;
            this.sessionId = response.sessionID;
            this.updateSolutionDisplay();
            this.showStatus(`Znaleziono rozwiązanie: ${this.solutionMoves.length} ruchów`, 'success');
        } else {
            this.showStatus('Nie udało się rozwiązać kostki', 'error');
        }
    }

    // Aktualizacja wyświetlania rozwiązania
    updateSolutionDisplay() {
        const container = document.getElementById('solution-moves');
        const playBtn = document.getElementById('play-solution-btn');
        const stepBtn = document.getElementById('step-solution-btn');

        container.innerHTML = '';

        if (this.solutionMoves.length === 0) {
            container.innerHTML = '<span style="color: #6c757d;">Brak rozwiązania</span>';
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
        stepBtn.disabled = false;
    }

    // Odtwarzanie rozwiązania
    async playSolution() {
        if (this.isPlayingSolution || this.currentMoveIndex >= this.solutionMoves.length) {
            return;
        }

        this.isPlayingSolution = true;
        const playBtn = document.getElementById('play-solution-btn');
        playBtn.textContent = '⏸️ Pause';
        playBtn.onclick = () => this.pauseSolution();

        while (this.currentMoveIndex < this.solutionMoves.length && this.isPlayingSolution) {
            const move = this.solutionMoves[this.currentMoveIndex];
            await this.makeMove(move);
            this.currentMoveIndex++;
            this.updateSolutionDisplay();
            
            // Krótka pauza między ruchami
            await new Promise(resolve => setTimeout(resolve, 500));
        }

        this.isPlayingSolution = false;
        playBtn.textContent = '▶️ Play Solution';
        playBtn.onclick = () => this.playSolution();
        
        if (this.currentMoveIndex >= this.solutionMoves.length) {
            // Sprawdź czy kostka jest rozwiązana i dodaj efekty
            if (this.isCubeSolved()) {
                this.showSolvedEffects();
            }
            this.showStatus('Rozwiązanie zakończone!', 'success');
        }
    }

    // Pauzowanie rozwiązania
    pauseSolution() {
        this.isPlayingSolution = false;
        const playBtn = document.getElementById('play-solution-btn');
        playBtn.textContent = '▶️ Play Solution';
        playBtn.onclick = () => this.playSolution();
        this.showStatus('Rozwiązanie wstrzymane', 'info');
    }

    // Następny krok rozwiązania
    async nextStep() {
        if (this.currentMoveIndex >= this.solutionMoves.length) {
            // Sprawdź czy kostka jest rozwiązana i dodaj efekty
            if (this.isCubeSolved()) {
                this.showSolvedEffects();
            }
            this.showStatus('Rozwiązanie zakończone!', 'success');
            return;
        }

        const move = this.solutionMoves[this.currentMoveIndex];
        await this.makeMove(move);
        this.currentMoveIndex++;
        this.updateSolutionDisplay();
    }

    // Wyświetlanie statusu
    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('status-message');
        const sessionElement = document.getElementById('session-info');
        
        statusElement.textContent = message;
        statusElement.className = `status-message ${type}`;
        
        if (this.sessionId) {
            sessionElement.textContent = `Session ID: ${this.sessionId}`;
        }
    }

    // Konfiguracja event listenerów
    setupEventListeners() {
        // Przyciski ruchów
        document.querySelectorAll('.move-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const move = e.target.dataset.move;
                this.makeMove(move);
            });
        });

        // Przycisk scramble
        document.getElementById('scramble-btn').addEventListener('click', () => {
            this.scrambleCube();
        });

        // Przycisk solve
        document.getElementById('solve-btn').addEventListener('click', () => {
            this.solveCube();
        });

        // Przycisk reset
        document.getElementById('reset-btn').addEventListener('click', () => {
            this.resetCube();
        });

        // Przycisk random
        document.getElementById('random-btn').addEventListener('click', () => {
            this.loadRandomCube();
        });

        // Przycisk play solution
        document.getElementById('play-solution-btn').addEventListener('click', () => {
            this.playSolution();
        });

        // Przycisk next step
        document.getElementById('step-solution-btn').addEventListener('click', () => {
            this.nextStep();
        });

        // Obsługa klawiatury
        document.addEventListener('keydown', (e) => {
            if (e.ctrlKey || e.metaKey) return; // Ignoruj skróty klawiszowe
            
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
                this.makeMove(move);
            }
        });
    }
}

// Inicjalizacja aplikacji po załadowaniu DOM
document.addEventListener('DOMContentLoaded', () => {
    new RubikCubeSolver();
});

// Dodatkowe style dla statusów
const style = document.createElement('style');
style.textContent = `
    .status-message.success { color: #28a745; }
    .status-message.error { color: #dc3545; }
    .status-message.info { color: #17a2b8; }
`;
document.head.appendChild(style);
