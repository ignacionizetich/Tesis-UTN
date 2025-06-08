function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.textContent = message;
    // Normaliza el tipo a minúsculas para comparar
    const isSuccess = type && type.toLowerCase() === 'success';
    toast.style.background = isSuccess ? '#4caf50' : '#f44336';
    toast.style.color = '#fff';
    toast.style.padding = '12px 24px';
    toast.style.marginTop = '10px';
    toast.style.borderRadius = '4px';
    toast.style.boxShadow = '0 2px 8px rgba(0,0,0,0.2)';
    toast.style.fontSize = '16px';
    toast.style.opacity = '0.95';
    toast.style.transition = 'opacity 0.5s';
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => container.removeChild(toast), 500);
    }, 2500);
}