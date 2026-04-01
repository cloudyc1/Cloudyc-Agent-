const API_BASE = '/api/chat';
const SESSION_API_BASE = '/api/chat/session';
let currentSessionId = null;
let currentSessionTitle = '';
let currentMode = 'NORMAL';
let knowledgePanelVisible = false;

function getUserId() {
    return document.getElementById('userId').value.trim() || 'user_001';
}

function toggleMode() {
    currentMode = currentMode === 'NORMAL' ? 'RAG' : 'NORMAL';
    updateModeUI();
    saveModePreference();
}

function updateModeUI() {
    const btn = document.getElementById('modeToggleBtn');
    const modeText = document.getElementById('modeText');
    
    if (currentMode === 'RAG') {
        btn.classList.add('rag-mode');
        modeText.textContent = '模式: RAG';
    } else {
        btn.classList.remove('rag-mode');
        modeText.textContent = '模式: Normal';
    }
}

function saveModePreference() {
    localStorage.setItem('chatMode', currentMode);
}

function loadModePreference() {
    const savedMode = localStorage.getItem('chatMode');
    if (savedMode && (savedMode === 'NORMAL' || savedMode === 'RAG')) {
        currentMode = savedMode;
        updateModeUI();
    }
}

function addSystemMessage(content) {
    const container = document.getElementById('chatMessages');
    const div = document.createElement('div');
    div.className = 'message message-system';
    
    const bubble = document.createElement('div');
    bubble.className = 'message-content system-message';
    bubble.innerHTML = content.replace(/\n/g, '<br>');
    
    const time = document.createElement('div');
    time.className = 'message-time';
    time.textContent = `系统 • ${new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}`;
    
    bubble.appendChild(time);
    div.appendChild(bubble);
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function toggleKnowledgePanel() {
    const panel = document.getElementById('knowledgePanel');
    knowledgePanelVisible = !knowledgePanelVisible;
    panel.classList.toggle('visible', knowledgePanelVisible);
    if (knowledgePanelVisible) {
        loadKnowledgeStats();
    }
}

async function uploadDocument(file) {
    const userId = getUserId();
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('file', file);

    try {
        console.log('开始上传文档，用户ID:', userId);
        const resp = await fetch('/api/chat/knowledge/upload', {
            method: 'POST',
            body: formData
        });
        const data = await resp.json();
        console.log('上传响应:', data);
        if (data.code === 200) {
            console.log('开始刷新知识库统计...');
            await loadKnowledgeStats();
            console.log('知识库统计刷新完成');
            alert('文档上传成功：' + data.data);
        } else {
            alert('上传失败：' + data.message);
        }
    } catch (e) {
        console.error('上传异常:', e);
        alert('上传失败：' + e.message);
    }
}

async function addTextToKnowledge(fileName, content) {
    const userId = getUserId();
    try {
        const resp = await fetch('/api/chat/knowledge/add', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({userId, fileName, content})
        });
        const data = await resp.json();
        if (data.code === 200) {
            alert('添加成功：' + data.data);
            await loadKnowledgeStats();
        } else {
            alert('添加失败：' + data.message);
        }
    } catch (e) {
        alert('添加失败：' + e.message);
    }
}

async function loadKnowledgeStats() {
    const userId = getUserId();
    console.log('加载知识库统计，用户ID:', userId);
    try {
        const [statsResp, docsResp] = await Promise.all([
            fetch(`/api/chat/knowledge/stats/${userId}`),
            fetch(`/api/chat/knowledge/documents/${userId}`)
        ]);
        const statsData = await statsResp.json();
        const docsData = await docsResp.json();
        console.log('统计数据:', statsData);
        console.log('文档数据:', docsData);

        if (statsData.code === 200) {
            document.getElementById('docCount').textContent = statsData.data.documentCount;
            document.getElementById('segmentCount').textContent = statsData.data.segmentCount;
        }

        if (docsData.code === 200) {
            renderDocumentList(docsData.data);
        }
    } catch (e) {
        console.error('加载统计失败:', e);
    }
}

function renderDocumentList(documents) {
    const container = document.getElementById('documentList');
    if (!documents || documents.length === 0) {
        container.innerHTML = '<div class="empty-state">暂无文档</div>';
        return;
    }

    container.innerHTML = documents.map(doc => {
        const uploadTime = doc.uploadTime ? new Date(doc.uploadTime).toLocaleString() : '未知时间';
        return `
            <div class="document-item" onclick="viewDocument('${escapeHtml(doc.fileName)}')">
                <div class="document-icon">📄</div>
                <div class="document-info">
                    <div class="document-name">${escapeHtml(doc.fileName)}</div>
                    <div class="document-meta">
                        <span>${doc.segmentCount} 个分段</span>
                        <span>• ${uploadTime}</span>
                    </div>
                </div>
                <button class="document-delete-btn" onclick="event.stopPropagation(); deleteDocument('${escapeHtml(doc.fileName)}')" title="删除文档">✕</button>
            </div>
        `;
    }).join('');
}

async function viewDocument(fileName) {
    const userId = getUserId();
    try {
        const resp = await fetch(`/api/chat/knowledge/document/${userId}?fileName=${encodeURIComponent(fileName)}`);
        const data = await resp.json();
        if (data.code === 200) {
            // 创建弹窗显示文档内容
            const modal = document.createElement('div');
            modal.className = 'document-modal';
            modal.innerHTML = `
                <div class="document-modal-content">
                    <div class="document-modal-header">
                        <h3>${escapeHtml(fileName)}</h3>
                        <button class="close-btn" onclick="this.closest('.document-modal').remove()">✕</button>
                    </div>
                    <div class="document-modal-body">
                        <pre>${escapeHtml(data.data)}</pre>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
        } else {
            alert('获取文档内容失败：' + data.message);
        }
    } catch (e) {
        alert('获取文档内容失败：' + e.message);
    }
}

async function deleteDocument(fileName) {
    if (!confirm(`确定删除文档 "${fileName}" 吗？此操作不可恢复！`)) return;

    const userId = getUserId();
    try {
        const resp = await fetch(`/api/chat/knowledge/document/${userId}?fileName=${encodeURIComponent(fileName)}`, {
            method: 'DELETE'
        });
        const data = await resp.json();
        if (data.code === 200) {
            alert('文档已删除');
            await loadKnowledgeStats();
        } else {
            alert('删除失败：' + data.message);
        }
    } catch (e) {
        alert('删除失败：' + e.message);
    }
}

async function clearKnowledge() {
    if (!confirm('确定清空知识库吗？此操作不可恢复！')) return;
    const userId = getUserId();
    try {
        const resp = await fetch(`/api/chat/knowledge/${userId}`, {method: 'DELETE'});
        const data = await resp.json();
        if (data.code === 200) {
            alert('知识库已清空');
            await loadKnowledgeStats();
        } else {
            alert('清空失败：' + data.message);
        }
    } catch (e) {
        alert('清空失败：' + e.message);
    }
}

function showAddTextDialog() {
    const fileName = prompt('请输入文件名：', 'knowledge.txt');
    if (!fileName) return;
    
    const content = prompt('请输入知识内容：');
    if (!content) return;
    
    addTextToKnowledge(fileName, content);
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
    let ts;
    if (timestamp) {
        try {
            ts = new Date(timestamp).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
        } catch (e) {
            ts = new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
        }
    } else {
        ts = new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
    }
    time.textContent = `${role === 'user' ? '我' : 'AI助手'} · ${ts}`;

    bubble.appendChild(time);
    div.appendChild(avatar);
    div.appendChild(bubble);
    container.appendChild(div);
    
    // 使用 setTimeout 确保 DOM 更新后再滚动
    setTimeout(() => {
        container.scrollTop = container.scrollHeight;
    }, 10);
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
                sessionId: currentSessionId,
                mode: currentMode
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
        console.log('加载会话:', sessionId);
        const [sessionResp, msgResp] = await Promise.all([
            fetch(`${SESSION_API_BASE}/${sessionId}`),
            fetch(`${API_BASE}/history/session/${sessionId}`)
        ]);
        const sessionData = await sessionResp.json();
        const msgData = await msgResp.json();

        console.log('会话数据:', sessionData);
        console.log('消息数据:', msgData);

        if (sessionData.code !== 200 || !sessionData.data) {
            alert('会话不存在或已过期');
            return;
        }

        currentSessionId = sessionId;
        currentSessionTitle = sessionData.data.title;
        document.getElementById('currentSessionTitle').textContent = currentSessionTitle;

        const messages = (msgData.code === 200 && msgData.data) ? msgData.data : [];
        console.log('消息列表类型:', typeof messages, Array.isArray(messages));
        console.log('消息列表:', messages);
        console.log('消息数量:', messages.length);
        
        const container = document.getElementById('chatMessages');
        container.innerHTML = '';

        if (!Array.isArray(messages) || messages.length === 0) {
            console.log('消息列表为空，显示提示');
            addMessage('ai', '该对话暂无消息记录。');
        } else {
            console.log('开始渲染消息，数量:', messages.length);
            messages.forEach((m, index) => {
                console.log(`消息 ${index}:`, m);
                // 处理 role 字段，支持多种格式
                let role = 'ai';
                if (m.role) {
                    const roleLower = m.role.toLowerCase();
                    if (roleLower === 'user' || roleLower === 'USER') {
                        role = 'user';
                    }
                }
                const content = m.content || m.message || '';
                const timestamp = m.timestamp || m.createTime;
                console.log(`添加消息 ${index}:`, {role, contentLength: content.length, timestamp});
                addMessage(role, content, timestamp);
            });
        }

        document.querySelectorAll('.session-item').forEach(i => i.classList.remove('active'));
        const active = document.querySelector(`.session-item[data-id="${sessionId}"]`);
        if (active) active.classList.add('active');

        // 强制滚动到底部
        setTimeout(() => {
            container.scrollTop = container.scrollHeight;
            console.log('滚动到:', container.scrollHeight);
        }, 100);
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

    document.getElementById('fileInput').addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            uploadDocument(file);
        }
        this.value = '';
    });

    checkHealth();
    setInterval(checkHealth, 30000);
    setTimeout(loadSessionList, 500);
    loadModePreference();
    input.focus();
});
