// ============================================
// AURA ORBIT - Bridge: JavaScript ↔ Kotlin
// ============================================

const AndroidBridge = window.AndroidBridge || {
    launchApp: (pkg) => console.log('Launch:', pkg),
    getInstalledApps: () => console.log('Get apps'),
    getAppIcon: (pkg) => null,
    onAppLaunched: (pkg) => {},
    onSphereTouched: (angle) => {},
    onPurchaseRequired: (feature) => {}
};

function roundRectBridge(ctx, x, y, w, h, r) {
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

function loadRealApps(appsData) {
    if (!appsData || appsData.length === 0) return;

    appSprites.forEach(sprite => {
        sphereGroup.remove(sprite);
        if (sprite.material.map) sprite.material.map.dispose();
        sprite.material.dispose();
    });
    appSprites = [];

    appsData.forEach((app, i) => {
        const phi = Math.acos(1 - 2 * (i + 0.5) / appsData.length);
        const theta = Math.PI * (1 + Math.sqrt(5)) * i;

        const x = CONFIG.sphereRadius * Math.sin(phi) * Math.cos(theta);
        const y = CONFIG.sphereRadius * Math.sin(phi) * Math.sin(theta);
        const z = CONFIG.sphereRadius * Math.cos(phi);

        const sprite = createRealAppSprite(app, i);
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

    console.log('Loaded ' + appsData.length + ' real apps');
}

function createRealAppSprite(app, index) {
    const canvas = document.createElement('canvas');
    canvas.width = 128;
    canvas.height = 128;
    const ctx = canvas.getContext('2d');

    const gradient = ctx.createRadialGradient(64, 64, 0, 64, 64, 64);
    gradient.addColorStop(0, 'rgba(34, 211, 238, 0.3)');
    gradient.addColorStop(0.7, 'rgba(107, 33, 168, 0.2)');
    gradient.addColorStop(1, 'transparent');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 128, 128);

    ctx.strokeStyle = 'rgba(34, 211, 238, 0.6)';
    ctx.lineWidth = 2;
    roundRectBridge(ctx, 8, 8, 112, 112, 20);
    ctx.stroke();

    const sprite = new THREE.Sprite(new THREE.SpriteMaterial({
        transparent: true,
        opacity: 0.9,
        blending: THREE.AdditiveBlending
    }));
    sprite.scale.set(40, 40, 1);

    if (app.iconBase64) {
        const img = new Image();
        img.onload = () => {
            ctx.drawImage(img, 16, 16, 96, 96);
            const texture = new THREE.CanvasTexture(canvas);
            if (sprite.material.map) sprite.material.map.dispose();
            sprite.material.map = texture;
            sprite.material.needsUpdate = true;
        };
        img.src = 'data:image/png;base64,' + app.iconBase64;
    } else {
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 48px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(app.name ? app.name.charAt(0).toUpperCase() : '?', 64, 64);
        sprite.material.map = new THREE.CanvasTexture(canvas);
    }

    sprite.onClick = () => {
        AndroidBridge.launchApp(app.packageName);
        AndroidBridge.onAppLaunched(app.packageName);
    };

    return sprite;
}

// Raycasting
const raycaster = new THREE.Raycaster();
const mouse = new THREE.Vector2();
let clickStartPos = { x: 0, y: 0 };
const CLICK_THRESHOLD = 10;

function onPointerDown(event) {
    const clientX = event.touches ? event.touches[0].clientX : event.clientX;
    const clientY = event.touches ? event.touches[0].clientY : event.clientY;
    clickStartPos = { x: clientX, y: clientY };
}

function onPointerUp(event) {
    const clientX = event.changedTouches ? event.changedTouches[0].clientX : event.clientX;
    const clientY = event.changedTouches ? event.changedTouches[0].clientY : event.clientY;

    const dx = clientX - clickStartPos.x;
    const dy = clientY - clickStartPos.y;
    const distance = Math.sqrt(dx * dx + dy * dy);

    if (distance > CLICK_THRESHOLD) return;

    const rect = renderer.domElement.getBoundingClientRect();
    mouse.x = ((clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((clientY - rect.top) / rect.height) * 2 + 1;

    raycaster.setFromCamera(mouse, camera);
    const intersects = raycaster.intersectObjects(appSprites);

    if (intersects.length > 0) {
        const sprite = intersects[0].object;
        if (sprite.onClick) sprite.onClick();

        sprite.scale.set(50, 50, 1);
        setTimeout(() => sprite.scale.set(40, 40, 1), 200);
    }
}

const glCanvas = document.getElementById('glCanvas');
glCanvas.addEventListener('mousedown', onPointerDown);
glCanvas.addEventListener('mouseup', onPointerUp);
glCanvas.addEventListener('touchstart', onPointerDown, { passive: true });
glCanvas.addEventListener('touchend', onPointerUp, { passive: true });

document.addEventListener('DOMContentLoaded', () => {
    setTimeout(() => {
        if (AndroidBridge.getInstalledApps) {
            AndroidBridge.getInstalledApps();
        }
    }, 500);
});

window.receiveApps = function(base64Json) {
    try {
        const jsonStr = atob(base64Json);
        const apps = JSON.parse(jsonStr);
        loadRealApps(apps);
    } catch (e) {
        console.error('Error parsing apps:', e);
    }
};

window.receiveAppIcon = function(packageName, base64Icon) {
    const sprite = appSprites.find(s =>
        s.userData.appData.packageName === packageName
    );
    if (sprite) {
        const canvas = document.createElement('canvas');
        canvas.width = 128;
        canvas.height = 128;
        const ctx = canvas.getContext('2d');
        const img = new Image();
        img.onload = () => {
            ctx.drawImage(img, 16, 16, 96, 96);
            if (sprite.material.map) sprite.material.map.dispose();
            sprite.material.map = new THREE.CanvasTexture(canvas);
            sprite.material.needsUpdate = true;
        };
        img.src = 'data:image/png;base64,' + base64Icon;
    }
};
