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
        
        this.initializeCube();
        this.setupEventListeners();
        this.resetCube();
        
        // Initialize default selected color without showing status message
        this.selectColorSilently('W');
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

    // Cube rendering
    renderCube() {
        const faces = ['up', 'front', 'right', 'left', 'back', 'down'];
        
        // Remove solved cube class if cube is not solved
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
                        square.addEventListener('click', (e) => {
                            this.paintSquare(faceName, row, col);
                        });
                    }
                    
                    faceElement.appendChild(square);
                }
            }
        });
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
            this.currentCube = response.cube;
            this.sessionId = response.sessionId;
            this.renderCube();
            this.showStatus(`Move ${move} executed`, 'success');
        } else {
            this.showStatus(`Error executing move: ${move}`, 'error');
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

    // Show solved cube effects
    showSolvedEffects() {
        // Add solved cube class
        const cubeContainer = document.querySelector('.cube-net');
        cubeContainer.classList.add('solved-cube');
        
        // Add solved cube animation
        this.animateSolvedCube();
        
        // Add visual success effect
        this.showSuccessEffect();
        
        this.showStatus('🎉 Congratulations! Cube has been solved!', 'success');
    }

    // Visual success effect
    showSuccessEffect() {
        // Add confetti effect (simple)
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
        
        // Remove after animation
        setTimeout(() => {
            if (confetti.parentNode) {
                confetti.parentNode.removeChild(confetti);
            }
        }, 2000);
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
        stepBtn.disabled = false;
    }

    // Solution playback
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
            
            // Short pause between moves
            await new Promise(resolve => setTimeout(resolve, 500));
        }

        this.isPlayingSolution = false;
        playBtn.textContent = '▶️ Play Solution';
        playBtn.onclick = () => this.playSolution();
        
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
        playBtn.textContent = '▶️ Play Solution';
        playBtn.onclick = () => this.playSolution();
        this.showStatus('Solution paused', 'info');
    }

    // Next solution step
    async nextStep() {
        if (this.currentMoveIndex >= this.solutionMoves.length) {
            // Check if cube is solved and add effects
            if (this.isCubeSolved()) {
                this.showSolvedEffects();
            }
            this.showStatus('Solution completed!', 'success');
            return;
        }

        const move = this.solutionMoves[this.currentMoveIndex];
        await this.makeMove(move);
        this.currentMoveIndex++;
        this.updateSolutionDisplay();
    }

    // Status display
    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('status-message');
        const sessionElement = document.getElementById('session-info');
        
        statusElement.textContent = message;
        statusElement.className = `status-message ${type}`;
        
        if (this.sessionId) {
            sessionElement.textContent = `Session ID: ${this.sessionId}`;
        }
    }

    // Event listeners configuration
    setupEventListeners() {
        // Move buttons
        document.querySelectorAll('.move-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const move = e.target.dataset.move;
                this.makeMove(move);
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

        // Play solution button
        document.getElementById('play-solution-btn').addEventListener('click', () => {
            this.playSolution();
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
                this.makeMove(move);
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
