/**
 * Real-Time Collaborative Communication Platform - Client Application
 */

const AppState = {
    currentUser: null,
    currentRoom: null,
    rooms: [],
    members: [],
    stompClient: null,
    roomSubMessages: null,
    roomSubEvents: null,
    presenceSub: null,
    typingTimer: null,
    isTyping: false,
    selectedPriority: 'NORMAL',
    selectedFilter: 'ALL',
    searchQuery: '',
    currentPage: 0,
    totalPages: 1
};

// Web Audio API Synth Alert Chimes
const SoundEffects = {
    playNotification(type) {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);

            if (type === 'URGENT') {
                osc.type = 'sawtooth';
                osc.frequency.setValueAtTime(880, ctx.currentTime);
                osc.frequency.setValueAtTime(440, ctx.currentTime + 0.15);
                osc.frequency.setValueAtTime(880, ctx.currentTime + 0.3);
                gain.gain.setValueAtTime(0.3, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.45);
                osc.start();
                osc.stop(ctx.currentTime + 0.45);
            } else if (type === 'IMPORTANT') {
                osc.type = 'sine';
                osc.frequency.setValueAtTime(587.33, ctx.currentTime);
                osc.frequency.setValueAtTime(880, ctx.currentTime + 0.12);
                gain.gain.setValueAtTime(0.25, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);
                osc.start();
                osc.stop(ctx.currentTime + 0.3);
            }
        } catch (e) {
            // AudioContext autoplay restrictions in some browsers
        }
    }
};

document.addEventListener('DOMContentLoaded', async () => {
    initEventListeners();
    await checkActuatorHealth();
    await loadRooms();
    connectWebSocket();

    // Clean any shared localStorage from previous versions
    try {
        localStorage.removeItem('chat_active_user_id');
    } catch (e) {}

    // Check if this specific tab has an authenticated session (sessionStorage is strictly per-tab)
    const tabUserId = sessionStorage.getItem('chat_tab_user_id');
    if (tabUserId) {
        try {
            const res = await fetch(`/api/users/${tabUserId}`);
            if (res.ok) {
                const user = await res.json();
                setCurrentUser(user, false);
            } else {
                sessionStorage.removeItem('chat_tab_user_id');
                openAuthModal();
            }
        } catch (e) {
            sessionStorage.removeItem('chat_tab_user_id');
            openAuthModal();
        }
    } else {
        // Every new tab starts with a fresh Login / Register prompt
        openAuthModal();
    }

    setInterval(checkActuatorHealth, 15000);
});

// ================= WebSocket & STOMP =================
function connectWebSocket() {
    const socket = new SockJS('/ws');
    AppState.stompClient = Stomp.over(socket);
    AppState.stompClient.debug = () => {}; // Disable verbose stomp console logs

    const headers = {};
    if (AppState.currentUser) {
        headers['userId'] = AppState.currentUser.id.toString();
    }

    AppState.stompClient.connect(headers, () => {
        // Subscribe to global presence topic
        AppState.presenceSub = AppState.stompClient.subscribe('/topic/presence', (message) => {
            const event = JSON.parse(message.body);
            handlePresenceEvent(event);
        });

        // Register presence explicitly
        if (AppState.currentUser) {
            registerPresence(AppState.currentUser);
        }

        // If active room selected, re-subscribe
        if (AppState.currentRoom) {
            subscribeToRoom(AppState.currentRoom.id);
        }
    }, (error) => {
        console.warn('STOMP connection error:', error);
        setTimeout(connectWebSocket, 5000);
    });
}

function registerPresence(user) {
    if (AppState.stompClient && AppState.stompClient.connected && user) {
        AppState.stompClient.send('/app/presence/register', {}, JSON.stringify({
            userId: user.id,
            username: user.username
        }));
    }
}

function subscribeToRoom(roomId) {
    if (!AppState.stompClient || !AppState.stompClient.connected) return;

    if (AppState.roomSubMessages) AppState.roomSubMessages.unsubscribe();
    if (AppState.roomSubEvents) AppState.roomSubEvents.unsubscribe();

    AppState.roomSubMessages = AppState.stompClient.subscribe(`/topic/rooms/${roomId}/messages`, (message) => {
        const event = JSON.parse(message.body);
        handleRoomMessageEvent(event);
    });

    AppState.roomSubEvents = AppState.stompClient.subscribe(`/topic/rooms/${roomId}/events`, (message) => {
        const event = JSON.parse(message.body);
        handleRoomEvent(event);
    });
}

// ================= Event Handlers =================
function handlePresenceEvent(event) {
    if (event.eventType === 'USER_ONLINE') {
        showToast(`${event.username} is now Online`, 'NORMAL');
    } else if (event.eventType === 'USER_OFFLINE') {
        showToast(`${event.username} went Offline`, 'NORMAL');
    }
    if (AppState.currentRoom) loadRoomMembers(AppState.currentRoom.id);
}

function handleRoomMessageEvent(event) {
    if (event.eventType === 'NEW_MESSAGE' || event.eventType === 'IMPORTANT_MESSAGE' || event.eventType === 'URGENT_MESSAGE') {
        renderMessageItem(event, true);
        if (event.priority === 'URGENT' || event.eventType === 'URGENT_MESSAGE') {
            SoundEffects.playNotification('URGENT');
            showPriorityBanner(event);
            showToast(`[URGENT] ${event.senderName}: ${event.content}`, 'URGENT');
        } else if (event.priority === 'IMPORTANT' || event.eventType === 'IMPORTANT_MESSAGE') {
            SoundEffects.playNotification('IMPORTANT');
            showToast(`[IMPORTANT] ${event.senderName}: ${event.content}`, 'IMPORTANT');
        }
    } else if (event.eventType === 'MESSAGE_UPDATED') {
        updateMessageDOM(event);
    } else if (event.eventType === 'MESSAGE_DELETED') {
        deleteMessageDOM(event);
    }
}

function handleRoomEvent(event) {
    if (event.eventType === 'ROOM_USER_JOINED') {
        renderSystemNotice(`${event.username} joined the room`);
        loadRoomMembers(AppState.currentRoom.id);
        loadRooms();
    } else if (event.eventType === 'ROOM_USER_LEFT') {
        renderSystemNotice(`${event.username} left the room`);
        loadRoomMembers(AppState.currentRoom.id);
        loadRooms();
    } else if (event.eventType === 'USER_TYPING') {
        if (!AppState.currentUser || event.userId !== AppState.currentUser.id) {
            showTypingIndicator(event.username);
        }
    } else if (event.eventType === 'USER_STOPPED_TYPING') {
        hideTypingIndicator();
    }
}

// ================= Authentication =================
function openAuthModal() {
    switchAuthTab('login');
    openModal('authModal');
}

function switchAuthTab(tab) {
    const loginBtn = document.getElementById('tabLoginBtn');
    const registerBtn = document.getElementById('tabRegisterBtn');
    const loginContent = document.getElementById('authLoginTabContent');
    const registerContent = document.getElementById('authRegisterTabContent');

    if (tab === 'login') {
        loginBtn.classList.add('active');
        registerBtn.classList.remove('active');
        loginContent.style.display = 'block';
        registerContent.style.display = 'none';
        setTimeout(() => {
            const input = document.getElementById('loginUsername');
            if (input) input.focus();
        }, 50);
    } else {
        registerBtn.classList.add('active');
        loginBtn.classList.remove('active');
        loginContent.style.display = 'none';
        registerContent.style.display = 'block';
        setTimeout(() => {
            const input = document.getElementById('regUsername');
            if (input) input.focus();
        }, 50);
    }
}

async function handleLoginUser(event) {
    event.preventDefault();
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!username || !password) return;

    try {
        const res = await fetch('/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (res.ok) {
            const user = await res.json();
            document.getElementById('loginUsername').value = '';
            document.getElementById('loginPassword').value = '';
            closeModal('authModal');
            setCurrentUser(user, true);
            showToast(`Welcome back, ${user.username}!`, 'NORMAL');
        } else {
            const err = await res.json();
            showToast(err.message || 'Invalid username/email or password', 'URGENT');
        }
    } catch (err) {
        showToast(err.message || 'Failed to connect to backend', 'URGENT');
    }
}

async function handleRegisterUser(event) {
    event.preventDefault();
    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;

    if (!username || !email || !password) return;

    try {
        const res = await fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, password })
        });

        if (res.ok) {
            const newUser = await res.json();
            document.getElementById('regUsername').value = '';
            document.getElementById('regEmail').value = '';
            document.getElementById('regPassword').value = '';
            closeModal('authModal');
            setCurrentUser(newUser, true);
            showToast(`Account created! Welcome, ${newUser.username}!`, 'NORMAL');
        } else {
            const err = await res.json();
            showToast(err.message || 'Registration failed', 'URGENT');
        }
    } catch (err) {
        showToast(err.message || 'Failed to connect to backend', 'URGENT');
    }
}

function setCurrentUser(user, showFeedback = true) {
    AppState.currentUser = user;
    sessionStorage.setItem('chat_tab_user_id', user.id);

    const nameEl = document.getElementById('currentUsername');
    const avatarEl = document.getElementById('currentUserAvatar');
    const subtextEl = document.getElementById('currentUserSubtext');
    const logoutBtn = document.getElementById('logoutBtn');

    if (nameEl) nameEl.textContent = user.username;
    if (avatarEl) avatarEl.textContent = user.username.substring(0, 2).toUpperCase();
    if (subtextEl) subtextEl.textContent = 'Active profile';
    if (logoutBtn) logoutBtn.style.display = 'inline-flex';

    registerPresence(user);
    if (AppState.currentRoom) {
        renderRoomActionButtons();
    }
}

function logoutUser() {
    sessionStorage.removeItem('chat_tab_user_id');
    AppState.currentUser = null;

    const nameEl = document.getElementById('currentUsername');
    const avatarEl = document.getElementById('currentUserAvatar');
    const subtextEl = document.getElementById('currentUserSubtext');
    const logoutBtn = document.getElementById('logoutBtn');

    if (nameEl) nameEl.textContent = 'Sign In / Register';
    if (avatarEl) avatarEl.textContent = '?';
    if (subtextEl) subtextEl.textContent = 'Click to login';
    if (logoutBtn) logoutBtn.style.display = 'none';

    if (AppState.currentRoom) {
        renderRoomActionButtons();
    }

    showToast('Logged out of current tab', 'NORMAL');
    openAuthModal();
}

function requireUserLogin() {
    if (!AppState.currentUser) {
        showToast('Please login or register to continue', 'IMPORTANT');
        openAuthModal();
        return false;
    }
    return true;
}

// ================= REST API Interactions =================
async function loadRooms() {
    try {
        let url = '/api/rooms';
        const params = new URLSearchParams();
        if (AppState.searchQuery) params.append('search', AppState.searchQuery);
        if (AppState.selectedFilter !== 'ALL') params.append('roomType', AppState.selectedFilter);
        if (params.toString()) url += `?${params.toString()}`;

        const res = await fetch(url);
        if (res.ok) {
            AppState.rooms = await res.json();
            renderRoomsList();
        }
    } catch (e) {
        console.error('Error loading rooms:', e);
    }
}

async function selectRoom(roomId) {
    try {
        const res = await fetch(`/api/rooms/${roomId}`);
        if (!res.ok) throw new Error('Room not found');
        AppState.currentRoom = await res.json();
        AppState.currentPage = 0;

        renderActiveRoomHeader();
        subscribeToRoom(roomId);
        await loadRoomMembers(roomId);
        await loadRoomMessages(roomId, 0, true);
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

async function loadRoomMembers(roomId) {
    try {
        const res = await fetch(`/api/rooms/${roomId}/members`);
        if (res.ok) {
            AppState.members = await res.json();
            renderMembersList();
            renderRoomActionButtons();
        }
    } catch (e) {
        console.error('Error loading members:', e);
    }
}

async function loadRoomMessages(roomId, page, replace = false) {
    try {
        const res = await fetch(`/api/rooms/${roomId}/messages?page=${page}&size=20&sort=createdAt,desc`);
        if (res.ok) {
            const data = await res.json();
            AppState.totalPages = data.totalPages;
            AppState.currentPage = page;

            const container = document.getElementById('messagesContainer');
            if (replace) container.innerHTML = '';

            const loadMoreBtn = document.getElementById('loadMoreBtn');
            if (loadMoreBtn) {
                loadMoreBtn.style.display = (AppState.currentPage < AppState.totalPages - 1) ? 'inline-block' : 'none';
            }

            // Reverse to show chronological order
            const messages = [...data.content].reverse();
            messages.forEach(msg => renderMessageItem(msg, false));

            if (replace) scrollToBottom();
        }
    } catch (e) {
        console.error('Error loading messages:', e);
    }
}

async function joinActiveRoom() {
    if (!requireUserLogin()) return;
    if (!AppState.currentRoom) return;

    try {
        const res = await fetch(`/api/rooms/${AppState.currentRoom.id}/join/${AppState.currentUser.id}`, {
            method: 'POST'
        });
        if (res.ok) {
            showToast(`Joined ${AppState.currentRoom.name}!`, 'NORMAL');
            await loadRoomMembers(AppState.currentRoom.id);
            await loadRooms();
        } else {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

async function leaveActiveRoom() {
    if (!requireUserLogin()) return;
    if (!AppState.currentRoom) return;

    try {
        const res = await fetch(`/api/rooms/${AppState.currentRoom.id}/leave/${AppState.currentUser.id}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast(`Left room ${AppState.currentRoom.name}`, 'NORMAL');
            await loadRoomMembers(AppState.currentRoom.id);
            await loadRooms();
        } else {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

async function deleteActiveRoom() {
    if (!requireUserLogin()) return;
    if (!AppState.currentRoom) return;
    if (!confirm(`Are you sure you want to delete room "${AppState.currentRoom.name}"?`)) return;

    try {
        const res = await fetch(`/api/rooms/${AppState.currentRoom.id}?userId=${AppState.currentUser.id}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast(`Room deleted successfully`, 'NORMAL');
            AppState.currentRoom = null;
            document.getElementById('chatMain').style.display = 'none';
            document.getElementById('noRoomSelected').style.display = 'flex';
            await loadRooms();
        } else {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

// ================= Message Actions =================
function sendMessage() {
    if (!requireUserLogin()) return;

    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    if (!content || !AppState.currentRoom) return;

    const payload = {
        senderId: AppState.currentUser.id,
        content: content,
        priority: AppState.selectedPriority
    };

    if (AppState.stompClient && AppState.stompClient.connected) {
        AppState.stompClient.send(`/app/rooms/${AppState.currentRoom.id}/send`, {}, JSON.stringify(payload));
    } else {
        fetch(`/api/rooms/${AppState.currentRoom.id}/messages`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(res => {
            if (!res.ok) res.json().then(err => showToast(err.message, 'URGENT'));
        });
    }

    input.value = '';
    sendStopTyping();
}

function handleTypingEvent() {
    if (!AppState.currentRoom || !AppState.currentUser) return;

    if (!AppState.isTyping) {
        AppState.isTyping = true;
        if (AppState.stompClient && AppState.stompClient.connected) {
            AppState.stompClient.send(`/app/rooms/${AppState.currentRoom.id}/typing`, {}, JSON.stringify({
                userId: AppState.currentUser.id,
                username: AppState.currentUser.username
            }));
        }
    }

    clearTimeout(AppState.typingTimer);
    AppState.typingTimer = setTimeout(sendStopTyping, 1500);
}

function sendStopTyping() {
    if (AppState.isTyping && AppState.currentRoom && AppState.currentUser) {
        AppState.isTyping = false;
        if (AppState.stompClient && AppState.stompClient.connected) {
            AppState.stompClient.send(`/app/rooms/${AppState.currentRoom.id}/stop-typing`, {}, JSON.stringify({
                userId: AppState.currentUser.id,
                username: AppState.currentUser.username
            }));
        }
    }
}

async function editMessagePrompt(messageId, currentContent) {
    if (!requireUserLogin()) return;
    const newContent = prompt('Edit your message:', currentContent);
    if (!newContent || newContent.trim() === '' || newContent === currentContent) return;

    try {
        const res = await fetch(`/api/messages/${messageId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: AppState.currentUser.id,
                content: newContent.trim()
            })
        });
        if (!res.ok) {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

async function deleteMessagePrompt(messageId) {
    if (!requireUserLogin()) return;
    if (!confirm('Are you sure you want to delete this message?')) return;
    try {
        const res = await fetch(`/api/messages/${messageId}?userId=${AppState.currentUser.id}`, {
            method: 'DELETE'
        });
        if (!res.ok) {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (e) {
        showToast(e.message, 'URGENT');
    }
}

// ================= UI Rendering =================
function renderRoomsList() {
    const container = document.getElementById('roomListContainer');
    container.innerHTML = '';

    if (AppState.rooms.length === 0) {
        container.innerHTML = `<div style="text-align:center; padding:24px 12px; color:var(--text-subtle); font-size:0.85rem;">No context rooms found</div>`;
        return;
    }

    AppState.rooms.forEach(room => {
        const card = document.createElement('div');
        const isActive = AppState.currentRoom && AppState.currentRoom.id === room.id;
        card.className = `room-card ${isActive ? 'active' : ''}`;
        card.innerHTML = `
            <div class="room-card-header">
                <span class="room-card-title">${escapeHtml(room.name)}</span>
                <span class="room-type-tag tag-${room.roomType}">${room.roomType}</span>
            </div>
            <div class="room-card-desc">${escapeHtml(room.description || 'No description provided')}</div>
            <div class="room-card-footer">
                <span>By ${escapeHtml(room.createdByName)}</span>
                <span>👥 ${room.memberCount} members</span>
            </div>
        `;
        card.onclick = () => selectRoom(room.id);
        container.appendChild(card);
    });
}

function renderActiveRoomHeader() {
    document.getElementById('noRoomSelected').style.display = 'none';
    document.getElementById('chatMain').style.display = 'flex';

    document.getElementById('activeRoomName').textContent = AppState.currentRoom.name;
    const tag = document.getElementById('activeRoomType');
    tag.textContent = AppState.currentRoom.roomType;
    tag.className = `room-type-tag tag-${AppState.currentRoom.roomType}`;

    document.getElementById('activeRoomDesc').textContent = AppState.currentRoom.description || '';
}

function renderRoomActionButtons() {
    const actions = document.getElementById('chatHeaderActions');
    actions.innerHTML = '';
    if (!AppState.currentRoom) return;

    if (!AppState.currentUser) {
        const joinBtn = document.createElement('button');
        joinBtn.className = 'btn-sm-action';
        joinBtn.textContent = 'Sign In to Join';
        joinBtn.onclick = openAuthModal;
        actions.appendChild(joinBtn);
        return;
    }

    const isMember = AppState.members.some(m => m.userId === AppState.currentUser.id && m.active);
    const isOwner = AppState.currentRoom.createdById === AppState.currentUser.id;

    if (!isMember) {
        const joinBtn = document.createElement('button');
        joinBtn.className = 'btn-sm-action';
        joinBtn.style.background = 'var(--accent-primary)';
        joinBtn.style.borderColor = 'var(--accent-primary)';
        joinBtn.style.color = '#fff';
        joinBtn.textContent = 'Join Room';
        joinBtn.onclick = joinActiveRoom;
        actions.appendChild(joinBtn);
    } else {
        const leaveBtn = document.createElement('button');
        leaveBtn.className = 'btn-sm-action';
        leaveBtn.textContent = 'Leave Room';
        leaveBtn.onclick = leaveActiveRoom;
        actions.appendChild(leaveBtn);
    }

    if (isOwner) {
        const delBtn = document.createElement('button');
        delBtn.className = 'btn-sm-action btn-sm-danger';
        delBtn.textContent = 'Delete Room';
        delBtn.onclick = deleteActiveRoom;
        actions.appendChild(delBtn);
    }
}

function renderMembersList() {
    const list = document.getElementById('membersList');
    list.innerHTML = '';
    document.getElementById('memberCountHeader').textContent = AppState.members.length;

    if (AppState.members.length === 0) {
        list.innerHTML = `<div style="text-align:center; padding:16px; color:var(--text-subtle); font-size:0.8rem;">No members in room</div>`;
        return;
    }

    AppState.members.forEach(m => {
        const item = document.createElement('div');
        item.className = 'member-item';
        item.innerHTML = `
            <div class="user-avatar">${(m.username || 'U').substring(0, 2).toUpperCase()}</div>
            <div class="member-info">
                <div class="member-name">${escapeHtml(m.username)}</div>
                <span class="member-role">${m.role}</span>
            </div>
            <span class="presence-dot"></span>
        `;
        list.appendChild(item);
    });
}

function renderMessageItem(msg, append = true) {
    const container = document.getElementById('messagesContainer');
    const isOwn = AppState.currentUser && msg.senderId === AppState.currentUser.id;

    const div = document.createElement('div');
    div.className = `message-item ${isOwn ? 'own' : ''}`;
    div.id = `msg-${msg.id || msg.messageId}`;

    const priority = msg.priority || 'NORMAL';
    const timeStr = msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    let actionsHtml = '';
    if (isOwn && !msg.deleted) {
        actionsHtml = `
            <div class="msg-actions">
                <button class="msg-action-btn" onclick="editMessagePrompt(${msg.id || msg.messageId}, '${escapeHtml(msg.content)}')">✏️</button>
                <button class="msg-action-btn" onclick="deleteMessagePrompt(${msg.id || msg.messageId})">🗑️</button>
            </div>
        `;
    }

    div.innerHTML = `
        <div class="msg-avatar">${(msg.senderName || 'U').substring(0, 2).toUpperCase()}</div>
        <div class="msg-body">
            <div class="msg-header">
                <span class="msg-sender">${escapeHtml(msg.senderName)}</span>
                <span class="msg-time">${timeStr}</span>
                <span class="priority-pill pill-${priority}">${priority}</span>
                ${msg.edited ? '<span class="msg-edited-tag">(edited)</span>' : ''}
            </div>
            <div class="msg-content-card priority-${priority}">
                ${actionsHtml}
                <div class="msg-text">${escapeHtml(msg.content)}</div>
            </div>
        </div>
    `;

    if (append) {
        container.appendChild(div);
        scrollToBottom();
    } else {
        container.insertBefore(div, container.firstChild);
    }
}

function updateMessageDOM(event) {
    const el = document.getElementById(`msg-${event.messageId}`);
    if (el) {
        const textEl = el.querySelector('.msg-text');
        if (textEl) textEl.textContent = event.content;
        const header = el.querySelector('.msg-header');
        if (header && !header.querySelector('.msg-edited-tag')) {
            const tag = document.createElement('span');
            tag.className = 'msg-edited-tag';
            tag.textContent = '(edited)';
            header.appendChild(tag);
        }
    }
}

function deleteMessageDOM(event) {
    const el = document.getElementById(`msg-${event.messageId}`);
    if (el) {
        const textEl = el.querySelector('.msg-text');
        if (textEl) {
            textEl.textContent = '[This message has been deleted]';
            textEl.style.fontStyle = 'italic';
            textEl.style.color = 'var(--text-subtle)';
        }
        const actions = el.querySelector('.msg-actions');
        if (actions) actions.remove();
    }
}

function renderSystemNotice(text) {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'system-notice';
    div.textContent = text;
    container.appendChild(div);
    scrollToBottom();
}

function showTypingIndicator(username) {
    const bar = document.getElementById('typingIndicatorBar');
    const textEl = document.getElementById('typingIndicatorText');
    if (textEl) textEl.textContent = `${username} is typing...`;
    bar.style.visibility = 'visible';
}

function hideTypingIndicator() {
    const bar = document.getElementById('typingIndicatorBar');
    bar.style.visibility = 'hidden';
}

function showPriorityBanner(event) {
    const banner = document.getElementById('priorityAlertBanner');
    banner.style.display = 'flex';
    document.getElementById('priorityAlertText').textContent = `🚨 [URGENT] ${event.senderName}: ${event.content}`;
}

function hidePriorityBanner() {
    document.getElementById('priorityAlertBanner').style.display = 'none';
}

function showToast(message, type = 'NORMAL') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    container.scrollTop = container.scrollHeight;
}

// ================= Actuator Health =================
async function checkActuatorHealth() {
    try {
        const res = await fetch('/actuator/health');
        if (res.ok) {
            const data = await res.json();
            const badge = document.getElementById('actuatorStatusBadge');
            if (badge) badge.textContent = `Backend ${data.status}`;
        }
    } catch (e) {
        const badge = document.getElementById('actuatorStatusBadge');
        if (badge) badge.textContent = 'Backend Offline';
    }
}

// ================= Event Listeners & Modals =================
function initEventListeners() {
    // Room Filter Pills
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            AppState.selectedFilter = btn.dataset.type;
            loadRooms();
        });
    });

    // Room Search
    document.getElementById('roomSearchInput').addEventListener('input', (e) => {
        AppState.searchQuery = e.target.value.trim();
        loadRooms();
    });

    // Priority Selector
    document.querySelectorAll('.priority-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.priority-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            AppState.selectedPriority = btn.dataset.priority;
        });
    });

    // Chat Input Enter & Typing
    const chatInput = document.getElementById('chatInput');
    chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendMessage();
        } else {
            handleTypingEvent();
        }
    });
}

function openCreateRoomModal() {
    if (!requireUserLogin()) return;
    openModal('createRoomModal');
}

async function handleCreateRoom(e) {
    e.preventDefault();
    if (!requireUserLogin()) return;

    const name = document.getElementById('newRoomName').value.trim();
    const description = document.getElementById('newRoomDesc').value.trim();
    const roomType = document.getElementById('newRoomType').value;

    try {
        const res = await fetch('/api/rooms', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: AppState.currentUser.id,
                name,
                description,
                roomType
            })
        });
        if (res.ok) {
            const newRoom = await res.json();
            showToast(`Room "${newRoom.name}" created!`, 'NORMAL');
            closeModal('createRoomModal');
            document.getElementById('newRoomName').value = '';
            document.getElementById('newRoomDesc').value = '';
            await loadRooms();
            selectRoom(newRoom.id);
        } else {
            const err = await res.json();
            showToast(err.message, 'URGENT');
        }
    } catch (err) {
        showToast(err.message, 'URGENT');
    }
}

function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}
