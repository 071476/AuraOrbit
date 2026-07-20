// ============================================
// AURA ORBIT - Esfera 3D de Aplicaciones
// ============================================

let scene, camera, renderer;
let sphereGroup, orbitRings = [];
let appSprites = [];
let isDragging = false;
let previousMousePosition = { x: 0, y: 0 };
let targetRotation = { x: 0, y: 0 };
let currentRotation = { x: 0, y: 0 };
let autoRotate = true;
let lastFrameTime = 0;

const CONFIG = {
    sphereRadius: 180,
    appCount: 24,
    orbitCount: 3,
    rotationSpeed: 0.002,
    dragSensitivity: 0.005,
    fpsIdle: 30,
    fpsActive: 60,
    colors: {
        ring1: 0x22D3EE,
        ring2: 0x6B21A8,
        ring3: 0xF472B6,
        glow: 0x22D3EE
    }
};

const APP_ICONS = [
    { name: 'WhatsApp', color: '#25D366', char: 'W' },
    { name: 'Instagram', color: '#E4405F', char: 'I' },
    { name: 'YouTube', color: '#FF0000', char: 'Y' },
    { name: 'Spotify', color: '#1DB954', char: 'S' },
    { name: 'Gmail', color: '#EA4335', char: 'G' },
    { name: 'Maps', color: '#4285F4', char: 'M' },
    { name: 'Chrome', color: '#4285F4', char: 'C' },
    { name: 'TikTok', color: '#000000', char: 'T' },
    { name: 'Netflix', color: '#E50914', char: 'N' },
    { name: 'Twitter', color: '#1DA1F2', char: 'X' },
    { name: 'Facebook', color: '#1877F2', char: 'F' },
    { name: 'Telegram', color: '#0088CC', char: 'Tg' },
    { name: 'Drive', color: '#4285F4', char: 'D' },
    { name: 'Photos', color: '#F4B400', char: 'P' },
    { name: 'Calendar', color: '#4285F4', char: 'Cal' },
    { name: 'Clock', color: '#757575', char: 'Clk' },
    { name: 'Settings', color: '#9E9E9E', char: 'Set' },
    { name: 'Camera', color: '#757575', char: 'Cam' },
    { name: 'Phone', color: '#4CAF50', char: 'Ph' },
    { name: 'Messages', color: '#4CAF50', char: 'Msg' },
    { name: 'Contacts', color: '#2196F3', char: 'Con' },
    { name: 'Files', color: '#FF9800', char: 'Fil' },
    { name: 'Weather', color: '#03A9F4', char: 'Wea' },
    { name: 'Notes', color: '#FFEB3B', char: 'Not' }
];

// ============================================
// Funcion roundRect compatible con WebViews antiguas
// ============================================
function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
}

function init() {
    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x050510, 0.0015);

    camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 2000);
    camera.position.z = 500;

    renderer = new THREE.WebGLRenderer({
        canvas: document.getElementById('glCanvas'),
        antialias: true,
        alpha: true
    });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x050510, 1);

    sphereGroup = new THREE.Group();
    scene.add(sphereGroup);

    createOrbitRings();
    createAppSphere();
    setupLighting();
    setupEvents();
    updateTime();
    setInterval(updateTime, 1000);
    animate();
}

function createOrbitRings() {
    const ringConfigs = [
        { radius: 220, tube: 0.5, color: CONFIG.colors.ring1, speed: 0.3 },
        { radius: 260, tube: 0.3, color: CONFIG.colors.ring2, speed: -0.2 },
        { radius: 300, tube: 0.4, color: CONFIG.colors.ring3, speed: 0.15 }
    ];

    ringConfigs.forEach((config, i) => {
        const geometry = new THREE.TorusGeometry(config.radius, config.tube, 16, 100);
        const material = new THREE.MeshBasicMaterial({
            color: config.color,
            transparent: true,
            opacity: 0.3,
            side: THREE.DoubleSide
        });

        const ring = new THREE.Mesh(geometry, material);
        ring.rotation.x = Math.PI / 2 + (Math.random() * 0.5);
        ring.rotation.y = Math.random() * Math.PI;
        ring.userData = { speed: config.speed, originalOpacity: 0.3 };

        orbitRings.push(ring);
        sphereGroup.add(ring);
        createRingParticles(config.radius, config.color);
    });
}

function createRingParticles(radius, color) {
    const particleCount = 50;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    const sizes = new Float32Array(particleCount);

    for (let i = 0; i < particleCount; i++) {
        const angle = (i / particleCount) * Math.PI * 2;
        const r = radius + (Math.random() - 0.5) * 20;
        positions[i * 3] = Math.cos(angle) * r;
        positions[i * 3 + 1] = (Math.random() - 0.5) * 10;
        positions[i * 3 + 2] = Math.sin(angle) * r;
        sizes[i] = Math.random() * 3 + 1;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1));

    const material = new THREE.PointsMaterial({
        color: color,
        size: 2,
        transparent: true,
        opacity: 0.6,
        blending: THREE.AdditiveBlending
    });

    const particles = new THREE.Points(geometry, material);
    sphereGroup.add(particles);
}

function createAppSphere() {
    APP_ICONS.forEach((app, i) => {
        const phi = Math.acos(1 - 2 * (i + 0.5) / CONFIG.appCount);
        const theta = Math.PI * (1 + Math.sqrt(5)) * i;

        const x = CONFIG.sphereRadius * Math.sin(phi) * Math.cos(theta);
        const y = CONFIG.sphereRadius * Math.sin(phi) * Math.sin(theta);
        const z = CONFIG.sphereRadius * Math.cos(phi);

        const sprite = createAppSprite(app, i);
        sprite.position.set(x, y, z);
        sprite.lookAt(0, 0, 0);

        sprite.userData = {
            originalPos: { x, y, z },
            appData: app,
            index: i,
            floatOffset: Math.random() * Math.PI * 2
        };

        appSprites.push(sprite);
        sphereGroup.add(sprite);
    });
}

function createAppSprite(app, index) {
    const canvas = document.createElement('canvas');
    canvas.width = 128;
    canvas.height = 128;
    const ctx = canvas.getContext('2d');

    const gradient = ctx.createRadialGradient(64, 64, 0, 64, 64, 64);
    gradient.addColorStop(0, app.color + '40');
    gradient.addColorStop(0.7, app.color + '20');
    gradient.addColorStop(1, 'transparent');

    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 128, 128);

    ctx.strokeStyle = app.color + '80';
    ctx.lineWidth = 2;
    roundRect(ctx, 8, 8, 112, 112, 20);
    ctx.stroke();

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 40px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(app.char, 64, 64);

    const texture = new THREE.CanvasTexture(canvas);

    const material = new THREE.SpriteMaterial({
        map: texture,
        transparent: true,
        opacity: 0.9,
        blending: THREE.AdditiveBlending
    });

    const sprite = new THREE.Sprite(material);
    sprite.scale.set(40, 40, 1);

    return sprite;
}

function setupLighting() {
    const ambientLight = new THREE.AmbientLight(0x404040, 0.5);
    scene.add(ambientLight);

    const pointLight1 = new THREE.PointLight(0x22D3EE, 1, 800);
    pointLight1.position.set(0, 0, 0);
    scene.add(pointLight1);

    const pointLight2 = new THREE.PointLight(0x6B21A8, 0.8, 600);
    pointLight2.position.set(300, 200, 200);
    scene.add(pointLight2);
}

function setupEvents() {
    const canvas = document.getElementById('glCanvas');

    canvas.addEventListener('touchstart', onTouchStart, { passive: false });
    canvas.addEventListener('touchmove', onTouchMove, { passive: false });
    canvas.addEventListener('touchend', onTouchEnd, { passive: false });

    canvas.addEventListener('mousedown', onMouseDown);
    canvas.addEventListener('mousemove', onMouseMove);
    canvas.addEventListener('mouseup', onMouseUp);

    window.addEventListener('resize', onWindowResize);
}

function onTouchStart(e) {
    e.preventDefault();
    autoRotate = false;
    isDragging = true;
    const touch = e.touches[0];
    previousMousePosition = { x: touch.clientX, y: touch.clientY };
}

function onTouchMove(e) {
    e.preventDefault();
    if (!isDragging) return;
    const touch = e.touches[0];
    handleDrag(touch.clientX, touch.clientY);
}

function onTouchEnd(e) {
    isDragging = false;
    setTimeout(() => { autoRotate = true; }, 3000);
}

function onMouseDown(e) {
    autoRotate = false;
    isDragging = true;
    previousMousePosition = { x: e.clientX, y: e.clientY };
}

function onMouseMove(e) {
    if (!isDragging) return;
    handleDrag(e.clientX, e.clientY);
}

function onMouseUp(e) {
    isDragging = false;
    setTimeout(() => { autoRotate = true; }, 3000);
}

function handleDrag(clientX, clientY) {
    const deltaX = clientX - previousMousePosition.x;
    const deltaY = clientY - previousMousePosition.y;

    targetRotation.y += deltaX * CONFIG.dragSensitivity;
    targetRotation.x += deltaY * CONFIG.dragSensitivity;

    previousMousePosition = { x: clientX, y: clientY };
}

function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}

function updateTime() {
    const now = new Date();

    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    document.getElementById('timeDisplay').textContent = `${hours}:${minutes}`;

    const options = {
        weekday: 'long',
        day: 'numeric',
        month: 'long'
    };
    const dateStr = now.toLocaleDateString('es-ES', options);
    document.getElementById('dateDisplay').textContent = dateStr;
}

// ============================================
// Loop optimizado: 30fps en reposo, 60fps al tocar
// ============================================
function animate(currentTime) {
    requestAnimationFrame(animate);

    // Determinar FPS objetivo
    const targetFPS = isDragging ? CONFIG.fpsActive : CONFIG.fpsIdle;
    const frameInterval = 1000 / targetFPS;

    // Saltar frame si no ha pasado suficiente tiempo
    const elapsed = currentTime - lastFrameTime;
    if (elapsed < frameInterval) return;
    lastFrameTime = currentTime;

    if (autoRotate) {
        targetRotation.y += CONFIG.rotationSpeed;
    }

    currentRotation.x += (targetRotation.x - currentRotation.x) * 0.05;
    currentRotation.y += (targetRotation.y - currentRotation.y) * 0.05;

    sphereGroup.rotation.x = currentRotation.x;
    sphereGroup.rotation.y = currentRotation.y;

    orbitRings.forEach((ring, i) => {
        ring.rotation.z += ring.userData.speed * 0.01;
        const pulse = Math.sin(Date.now() * 0.001 + i) * 0.1 + 0.3;
        ring.material.opacity = pulse;
    });

    const time = Date.now() * 0.001;
    appSprites.forEach((sprite, i) => {
        const offset = sprite.userData.floatOffset;
        const floatY = Math.sin(time + offset) * 5;
        sprite.position.y = sprite.userData.originalPos.y + floatY;

        const depth = (sprite.position.z + CONFIG.sphereRadius) / (2 * CONFIG.sphereRadius);
        const scale = 0.6 + depth * 0.4;
        sprite.scale.set(40 * scale, 40 * scale, 1);

        sprite.material.opacity = 0.4 + depth * 0.6;
    });

    renderer.render(scene, camera);
}

document.addEventListener('DOMContentLoaded', init);
