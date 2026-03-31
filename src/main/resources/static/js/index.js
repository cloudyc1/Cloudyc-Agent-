const API_BASE = '/api/chat';
const SESSION_API_BASE = '/api/chat/session';
let currentSessionId = null;
let currentSessionTitle = '';

function getUserId() {
    return document.getElementById('userId').value.trim() || 'user_001';
}

function setStatus(online) {
    const dot = document.getElementById('statusDot');
    const text = document.getElementById('apiStatusText');
    if (online) {
        dot.classList.remove('offline');
        text.textContent = '在线';
    } else {
        dot.classList.add('offline');
        text.textContent = '离线';
    }
}

function addMessage(role, content, timestamp) {
    const container = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = `message message-${role}`;

    const avatar = document.createElement('div');
    avatar.className = `message-avatar ${role === 'user' ? 'user-avatar' : 'ai-avatar'}`;
    avatar.textContent = role === 'user' ? '👤' : '🤖';

    const bubble = document.createElement('div');
    bubble.className = 'message-content';
    bubble.innerHTML = content.replace(/\n/g, '<br>');

    const time = document.createElement('div');
    time.className = 'message-time';
    const ts = timestamp
        ? new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
        : new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
    time.textContent = `${role === 'user' ? '我' : 'AI助手'} · ${ts}`;

    bubble.appendChild(time);
    div.appendChild(avatar);
    div.appendChild(bubble);
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function showLoading() {
    const container = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = 'message message-ai';
    div.id = 'loadingMsg';
    div.innerHTML = `
        <div class="message-avatar ai-avatar">🤖</div>
        <div class="message-content loading-message">
            AI正在思考 <div class="loading-dots"><span></span><span></span><span></span></div>
        </div>`;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function hideLoading() {
    const el = document.getElementById('loadingMsg');
    if (el) el.remove();
}

function clearChat() {
    document.getElementById('chatMessages').innerHTML = `
        <div class="message message-ai">
            <div class="message-avatar ai-avatar">🤖</div>
            <div class="message-content">
                开始新的对话吧！<br>当前对话将被保存到历史记录中。
                <div class="message-time">系统消息 · 刚刚</div>
            </div>
        </div>`;
}

function startNewChat() {
    currentSessionId = null;
    currentSessionTitle = '';
    document.getElementById('currentSessionTitle').textContent = '';
    clearChat();
    document.getElementById('messageInput').focus();
    document.querySelectorAll('.session-item').forEach(i => i.classList.remove('active'));
}

async function sendMessage() {
    const input = document.getElementById('messageInput');
    const btn = document.getElementById('sendButton');
    const msg = input.value.trim();
    if (!msg) return;

    const userId = getUserId();
    addMessage('user', msg);
    input.value = '';
    input.style.height = 'auto';
    btn.disabled = true;
    showLoading();

    try {
        const resp = await fetch(`${API_BASE}/send/session`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                userId: userId,
                message: msg,
                sessionId: currentSessionId
            })
        });
        const data = await resp.json();
        hideLoading();

        if (data.code === 200) {
            const result = data.data;
            addMessage('ai', result.response);
            setStatus(true);

            if (result.sessionId && result.sessionId !== currentSessionId) {
                currentSessionId = result.sessionId;
                setTimeout(() => loadSessionList(), 600);
            }
            if (!currentSessionTitle && currentSessionId) {
                setTimeout(() => updateTitle(), 800);
            }
        } else {
            addMessage('ai', '请求失败：' + (data.message || '未知错误'));
            setStatus(false);
        }
    } catch (e) {
        hideLoading();
        addMessage('ai', '网络连接异常，请检查网络后重试');
        setStatus(false);
    } finally {
        btn.disabled = false;
        input.focus();
    }
}

async function updateTitle() {
    if (!currentSessionId) return;
    try {
        const resp = await fetch(`${SESSION_API_BASE}/${currentSessionId}`);
        const data = await resp.json();
        if (data.code === 200 && data.data) {
            currentSessionTitle = data.data.title;
            document.getElementById('currentSessionTitle').textContent = currentSessionTitle;
        }
    } catch (e) {
        console.error('获取标题失败:', e);
    }
}

async function loadSessionList() {
    const userId = getUserId();
    const el = document.getElementById('sessionList');
    try {
        const resp = await fetch(`${SESSION_API_BASE}/list/${userId}`);
        const data = await resp.json();

        if (data.code === 200 && data.data && data.data.length > 0) {
            const sessions = data.data;
            el.innerHTML = sessions.map((s, i) => `
                <div class="session-item ${s.sessionId === currentSessionId ? 'active' : ''}"
                     data-id="${s.sessionId}" onclick="loadSession('${s.sessionId}')">
                    <div class="session-item-icon">💬</div>
                    <div class="session-item-info">
                        <div class="session-item-title">${escapeHtml(s.title)}</div>
                        <div class="session-item-meta">
                            <span>${formatDate(s.updateTime)}</span>
                            <span class="session-item-count">${s.messageCount}条</span>
                        </div>
                    </div>
                    <button class="session-item-delete"
                            onclick="event.stopPropagation(); deleteSession('${s.sessionId}')">✕</button>
                </div>`).join('');
        } else {
            el.innerHTML = '<div class="session-empty">暂无历史对话<br>发送消息后自动创建</div>';
        }
    } catch (e) {
        el.innerHTML = '<div class="session-empty">加载失败</div>';
    }
}

async function loadSession(sessionId) {
    try {
        const [sessionResp, msgResp] = await Promise.all([
            fetch(`${SESSION_API_BASE}/${sessionId}`),
            fetch(`${API_BASE}/history/session/${sessionId}`)
        ]);
        const sessionData = await sessionResp.json();
        const msgData = await msgResp.json();

        if (sessionData.code !== 200 || !sessionData.data) {
            alert('会话不存在或已过期');
            return;
        }

        currentSessionId = sessionId;
        currentSessionTitle = sessionData.data.title;
        document.getElementById('currentSessionTitle').textContent = currentSessionTitle;

        const messages = (msgData.code === 200 && msgData.data) ? msgData.data : [];
        const container = document.getElementById('chatMessages');
        container.innerHTML = '';

        if (messages.length === 0) {
            addMessage('ai', '该对话暂无消息记录。');
        } else {
            messages.forEach(m => {
                addMessage(m.role === 'user' ? 'user' : 'ai', m.content, m.timestamp);
            });
        }

        document.querySelectorAll('.session-item').forEach(i => i.classList.remove('active'));
        const active = document.querySelector(`.session-item[data-id="${sessionId}"]`);
        if (active) active.classList.add('active');

        container.scrollTop = container.scrollHeight;
    } catch (e) {
        console.error('加载会话失败:', e);
        alert('加载会话失败，请重试');
    }
}

async function deleteSession(sessionId) {
    if (!confirm('确定删除这个对话吗？')) return;
    try {
        const resp = await fetch(`${SESSION_API_BASE}/${sessionId}?userId=${getUserId()}`, {method: 'DELETE'});
        const data = await resp.json();
        if (data.code === 200) {
            if (sessionId === currentSessionId) startNewChat();
            loadSessionList();
        } else {
            alert('删除失败');
        }
    } catch (e) {
        alert('删除失败');
    }
}

async function clearHistory() {
    if (!confirm('确定清除当前对话历史吗？')) return;
    try {
        const id = currentSessionId || getUserId();
        await fetch(`${API_BASE}/history/${id}`, {method: 'DELETE'});
        clearChat();
        alert('对话历史已清除');
    } catch (e) {
        alert('清除失败');
    }
}

async function getHistory() {
    if (!currentSessionId) {
        alert('当前没有进行中的对话，请先发送消息或选择历史对话');
        return;
    }
    try {
        const resp = await fetch(`${API_BASE}/history/session/${currentSessionId}`);
        const data = await resp.json();
        const msgs = (data.code === 200 && data.data) ? data.data : [];
        if (msgs.length === 0) { alert('当前对话暂无消息'); return; }

        let text = `当前对话记录 (共${msgs.length}条)\n${'='.repeat(30)}\n\n`;
        msgs.forEach((m, i) => {
            const role = m.role === 'user' ? '👤 用户' : '🤖 AI';
            text += `${i + 1}. [${role}]\n${m.content}\n\n${'─'.repeat(20)}\n\n`;
        });
        alert(text);
    } catch (e) {
        alert('获取失败');
    }
}

async function getStats() {
    try {
        const resp = await fetch(`${API_BASE}/stats/${getUserId()}`);
        const s = await resp.json();
        alert(
            `📊 用户对话统计\n${'='.repeat(25)}\n\n` +
            `👤 用户ID: ${s.userId || getUserId()}\n` +
            `🧠 上下文记录数: ${s.historySize || 0}\n` +
            `⏰ 时间: ${new Date(s.timestamp || Date.now()).toLocaleString()}`
        );
    } catch (e) {
        alert('获取统计失败');
    }
}

function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t;
    return d.innerHTML;
}

function formatDate(str) {
    if (!str) return '';
    const d = new Date(str);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
        return d.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
    }
    return d.toLocaleDateString([], {month: 'short', day: 'numeric'});
}

async function checkHealth() {
    try {
        const resp = await fetch(`${API_BASE}/health`);
        const data = await resp.json();
        setStatus(data.status === 'UP');
    } catch (e) {
        setStatus(false);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const input = document.getElementById('messageInput');

    input.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';
    });

    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    document.getElementById('userId').addEventListener('change', function() {
        startNewChat();
        loadSessionList();
    });

    checkHealth();
    setInterval(checkHealth, 30000);
    setTimeout(loadSessionList, 500);
    input.focus();
});
