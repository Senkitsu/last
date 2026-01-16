// Configuration
const API_BASE_URL = 'http://localhost:8080/api';
let accessToken = localStorage.getItem('accessToken');
let currentUser = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    if (accessToken) {
        showMainApp();
        loadDashboard();
    } else {
        showLoginSection();
    }
});

// ==================== AUTH ====================

function login(event) {
    event.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    .then(res => res.json())
    .then(data => {
        if (data.accessToken) {
            accessToken = data.accessToken;
            localStorage.setItem('accessToken', accessToken);
            showMainApp();
            loadDashboard();
        } else {
            showError('loginError', 'Login failed: ' + (data.message || 'Invalid credentials'));
        }
    })
    .catch(err => showError('loginError', 'Login error: ' + err.message));
}

function logout() {
    fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: getHeaders()
    })
    .then(() => {
        accessToken = null;
        localStorage.removeItem('accessToken');
        showLoginSection();
    });
}

function showLoginSection() {
    document.getElementById('login-section').classList.add('active');
    document.querySelectorAll('.section:not(#login-section)').forEach(el => el.classList.remove('active'));
    document.querySelector('nav').style.display = 'none';
    document.querySelector('.sidebar').style.display = 'none';
}

function showMainApp() {
    document.getElementById('login-section').classList.remove('active');
    document.querySelector('nav').style.display = 'block';
    document.querySelector('.sidebar').style.display = 'block';
    loadUserInfo();
}

function loadUserInfo() {
    fetch(`${API_BASE_URL}/auth/info`, { headers: getHeaders() })
    .then(res => res.json())
    .then(data => {
        currentUser = data;
        document.getElementById('userInfo').textContent = `Welcome, ${data.username}!`;
    });
}

// ==================== NAVIGATION ====================

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(el => el.classList.remove('active'));
    document.getElementById(sectionId).classList.add('active');

    document.querySelectorAll('.nav-link').forEach(el => el.classList.remove('active'));
    event.target.closest('.nav-link').classList.add('active');

    if (sectionId === 'buses') loadBuses();
    else if (sectionId === 'sensors') loadSensors();
    else if (sectionId === 'dashboard') loadDashboard();
}

// ==================== DASHBOARD ====================

function loadDashboard() {
    Promise.all([
        fetch(`${API_BASE_URL}/buses`, { headers: getHeaders() }).then(r => r.json()),
        fetch(`${API_BASE_URL}/sensors`, { headers: getHeaders() }).then(r => r.json())
    ])
    .then(([buses, sensors]) => {
        document.getElementById('totalBuses').textContent = buses.length;
        document.getElementById('totalSensors').textContent = sensors.length;
        document.getElementById('totalAnomalies').textContent = sensors.filter(s => s.anomaly).length;
        document.getElementById('totalFiles').textContent = sensors.filter(s => s.filePath).length;
    });
}

// ==================== BUSES ====================

function loadBuses() {
    showLoader('busesLoader', true);
    fetch(`${API_BASE_URL}/buses`, { headers: getHeaders() })
    .then(res => res.json())
    .then(buses => {
        const tbody = document.getElementById('busesList');
        tbody.innerHTML = '';
        buses.forEach(bus => {
            tbody.innerHTML += `
                <tr>
                    <td>${bus.id}</td>
                    <td>${bus.model}</td>
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="openEditBusModal(${bus.id}, '${bus.model}')">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteBus(${bus.id})">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `;
        });
        showLoader('busesLoader', false);
    });
}

function addBus(event) {
    event.preventDefault();
    const model = document.getElementById('busModel').value;

    fetch(`${API_BASE_URL}/buses`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ model })
    })
    .then(res => res.json())
    .then(() => {
        loadBuses();
        document.getElementById('busModel').value = '';
        showToast('Bus added successfully!');
    });
}

function openEditBusModal(id, model) {
    document.getElementById('editBusId').value = id;
    document.getElementById('editBusModel').value = model;
    new bootstrap.Modal(document.getElementById('editBusModal')).show();
}

function updateBus(event) {
    event.preventDefault();
    const id = document.getElementById('editBusId').value;
    const model = document.getElementById('editBusModel').value;

    fetch(`${API_BASE_URL}/buses/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify({ model })
    })
    .then(() => {
        loadBuses();
        bootstrap.Modal.getInstance(document.getElementById('editBusModal')).hide();
        showToast('Bus updated successfully!');
    });
}

function deleteBus(id) {
    if (confirm('Are you sure?')) {
        fetch(`${API_BASE_URL}/buses/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        })
        .then(() => {
            loadBuses();
            showToast('Bus deleted!');
        });
    }
}

// ==================== SENSORS ====================

function loadSensors() {
    showLoader('sensorsLoader', true);
    fetch(`${API_BASE_URL}/sensors`, { headers: getHeaders() })
    .then(res => res.json())
    .then(sensors => {
        const tbody = document.getElementById('sensorsList');
        tbody.innerHTML = '';
        sensors.forEach(sensor => {
            tbody.innerHTML += `
                <tr>
                    <td>${sensor.id}</td>
                    <td>${sensor.bus.id}</td>
                    <td>${sensor.sensorType}</td>
                    <td>${sensor.value}</td>
                    <td>${formatDate(sensor.timestamp)}</td>
                    <td>
                        ${sensor.anomaly ? '<span class="badge bg-danger">Yes</span>' : '<span class="badge bg-success">No</span>'}
                    </td>
                    <td>
                        ${sensor.filePath ? `<a href="${API_BASE_URL}/files/download/${sensor.filePath}" class="btn btn-sm btn-info"><i class="fas fa-download"></i></a>` : '-'}
                    </td>
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="openEditSensorModal(${sensor.id}, '${sensor.sensorType}', ${sensor.value}, ${sensor.anomaly})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteSensor(${sensor.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        });
        showLoader('sensorsLoader', false);
    });
}

function addSensorData(event) {
    event.preventDefault();
    const busId = parseInt(document.getElementById('sensorBusId').value);
    const sensorType = document.getElementById('sensorType').value;
    const value = parseFloat(document.getElementById('sensorValue').value);
    const timestamp = new Date().toISOString();
    const anomaly = document.getElementById('sensorAnomaly').value === 'true';

    fetch(`${API_BASE_URL}/sensors`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ busId, sensorType, value, timestamp, anomaly })
    })
    .then(res => res.json())
    .then(() => {
        loadSensors();
        document.getElementById('sensorBusId').value = '';
        document.getElementById('sensorValue').value = '';
        showToast('Sensor data added successfully!');
    });
}

function openEditSensorModal(id, sensorType, value, anomaly) {
    document.getElementById('editSensorId').value = id;
    document.getElementById('editSensorType').value = sensorType;
    document.getElementById('editSensorValue').value = value;
    document.getElementById('editSensorAnomaly').value = anomaly;
    new bootstrap.Modal(document.getElementById('editSensorModal')).show();
}

function updateSensor(event) {
    event.preventDefault();
    const id = document.getElementById('editSensorId').value;
    const sensorType = document.getElementById('editSensorType').value;
    const value = parseFloat(document.getElementById('editSensorValue').value);
    const anomaly = document.getElementById('editSensorAnomaly').value === 'true';

    fetch(`${API_BASE_URL}/sensors/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify({ sensorType, value, anomaly })
    })
    .then(() => {
        loadSensors();
        bootstrap.Modal.getInstance(document.getElementById('editSensorModal')).hide();
        showToast('Sensor updated successfully!');
    });
}

function deleteSensor(id) {
    if (confirm('Are you sure?')) {
        fetch(`${API_BASE_URL}/sensors/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        })
        .then(() => {
            loadSensors();
            showToast('Sensor deleted!');
        });
    }
}

// ==================== FILE UPLOAD ====================

function handleDragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    document.getElementById('fileUploadZone').classList.add('dragover');
}

function handleDragLeave(event) {
    event.preventDefault();
    document.getElementById('fileUploadZone').classList.remove('dragover');
}

function handleFileDrop(event) {
    event.preventDefault();
    document.getElementById('fileUploadZone').classList.remove('dragover');
    const files = event.dataTransfer.files;
    if (files.length > 0) {
        document.getElementById('fileInput').files = files;
        uploadFile();
    }
}

function uploadFile() {
    const sensorId = document.getElementById('fileSensorId').value;
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];

    if (!sensorId) {
        showError('uploadResult', 'Please enter Sensor Data ID');
        return;
    }

    if (!file) {
        showError('uploadResult', 'Please select a file');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    document.getElementById('uploadProgress').style.display = 'block';

    fetch(`${API_BASE_URL}/files/upload/${sensorId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${accessToken}` },
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('uploadProgress').style.display = 'none';
        showSuccess('uploadResult', `File uploaded successfully! Stored as: ${data.storedFilename}`);
        document.getElementById('fileInput').value = '';
        document.getElementById('fileSensorId').value = '';
        loadSensors();
    })
    .catch(err => {
        document.getElementById('uploadProgress').style.display = 'none';
        showError('uploadResult', 'Upload failed: ' + err.message);
    });
}

// ==================== CSV IMPORT ====================

function handleCsvDrop(event) {
    event.preventDefault();
    document.getElementById('csvUploadZone').classList.remove('dragover');
    const files = event.dataTransfer.files;
    if (files.length > 0) {
        document.getElementById('csvInput').files = files;
        importCsv();
    }
}

function importCsv() {
    const fileInput = document.getElementById('csvInput');
    const file = fileInput.files[0];

    if (!file) {
        showError('csvResult', 'Please select a CSV file');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    document.getElementById('csvProgress').style.display = 'block';

    fetch(`${API_BASE_URL}/sensors/import-csv`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${accessToken}` },
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('csvProgress').style.display = 'none';
        if (data.failedCount > 0) {
            let errors = '<h5 class="text-warning">Import Completed with Errors:</h5>';
            errors += `<p><strong>Success:</strong> ${data.successCount} | <strong>Failed:</strong> ${data.failedCount}</p>`;
            errors += '<ul>';
            data.errors.forEach(error => errors += `<li>${error}</li>`);
            errors += '</ul>';
            document.getElementById('csvResult').innerHTML = errors;
        } else {
            showSuccess('csvResult', `CSV import successful! ${data.successCount} records imported.`);
        }
        document.getElementById('csvInput').value = '';
        loadSensors();
    })
    .catch(err => {
        document.getElementById('csvProgress').style.display = 'none';
        showError('csvResult', 'Import failed: ' + err.message);
    });
}

// ==================== HELPERS ====================

function getHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    };
}

function showLoader(id, show) {
    document.getElementById(id).classList.toggle('active', show);
}

function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'alert alert-success position-fixed bottom-0 end-0 m-3';
    toast.innerHTML = `<i class="fas fa-check-circle"></i> ${message}`;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function showSuccess(elementId, message) {
    document.getElementById(elementId).innerHTML = `<div class="alert alert-success"><i class="fas fa-check-circle"></i> ${message}</div>`;
}

function showError(elementId, message) {
    document.getElementById(elementId).innerHTML = `<div class="alert alert-danger"><i class="fas fa-exclamation-circle"></i> ${message}</div>`;
}

function formatDate(dateString) {
    return new Date(dateString).toLocaleString();
}