const API_BASE = '/api/chat';
let messageCount = 0;

// 获取用户ID
function getUserId() {
    const userId = document.getElementById('userId').value.trim();
    return userId || 'user_001';
}

// 更新上下文指示器
function updateContextIndicator(count) {
    document.getElementById('contextCount').textContent = count;
    document.getElementById('contextCountDisplay').textContent = count;
}

// 更新消息计数
function updateMessageCount() {
    messageCount++;
    document.getElementById('messageCount').textContent = messageCount;
}

// 更新最后活动时间
function updateLastActivity() {
    const now = new Date();
    const timeStr = now.getHours().toString().padStart(2, '0') + ':' +
        now.getMinutes().toString().padStart(2, '0');
    document.getElementById('lastActivity').textContent = timeStr;
}

// 设置状态
function setStatus(elementId, status, text) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = text;
        // 可以根据状态添加不同的样式
        if (status === 'online') {
            element.style.color = '#10b981';
        } else if (status === 'offline') {
            element.style.color = '#ef4444';
        } else {
            element.style.color = '#60a5fa';
        }
    }
}

// 添加消息到聊天窗口
function addMessage(role, content) {
    const messagesContainer = document.getElementById('chatMessages');

    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${role}`;

    const avatarDiv = document.createElement('div');
    avatarDiv.className = `message-avatar ${role === 'user' ? 'user-avatar' : 'ai-avatar'}`;
    avatarDiv.textContent = role === 'user' ? '👤' : '🤖';

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';

    // 处理换行和基本格式化
    const formattedContent = content.replace(/\n/g, '<br>');
    contentDiv.innerHTML = formattedContent;

    const timeDiv = document.createElement('div');
    timeDiv.className = 'message-time';
    timeDiv.textContent = `${role === 'user' ? '我' : 'AI助手'} • ${new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;

    contentDiv.appendChild(timeDiv);

    messageDiv.appendChild(avatarDiv);
    messageDiv.appendChild(contentDiv);
    messagesContainer.appendChild(messageDiv);

    // 滚动到底部
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    // 更新计数
    if (role === 'user') {
        updateMessageCount();
    }
    updateLastActivity();
}

// 添加加载消息
function addLoadingMessage() {
    const messagesContainer = document.getElementById('chatMessages');

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message message-ai';
    messageDiv.id = 'loadingMessage';

    const avatarDiv = document.createElement('div');
    avatarDiv.className = 'message-avatar ai-avatar';
    avatarDiv.textContent = '🤖';

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content loading-message';
    contentDiv.innerHTML = 'AI助手正在思考中 <div class="loading-dots"><span></span><span></span><span></span></div>';

    messageDiv.appendChild(avatarDiv);
    messageDiv.appendChild(contentDiv);
    messagesContainer.appendChild(messageDiv);

    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

// 移除加载消息
function removeLoadingMessage() {
    const loadingMessage = document.getElementById('loadingMessage');
    if (loadingMessage) {
        loadingMessage.remove();
    }
}

// 发送消息
async function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const sendButton = document.getElementById('sendButton');
    const message = messageInput.value.trim();

    if (!message) {
        alert('请输入消息内容');
        return;
    }

    const userId = getUserId();
    if (!userId) {
        alert('请输入用户标识符');
        return;
    }

    // 添加用户消息
    addMessage('user', message);
    messageInput.value = '';

    // 禁用发送按钮并添加加载消息
    sendButton.disabled = true;
    addLoadingMessage();

    try {
        const response = await fetch(`${API_BASE}/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId, message: message })
        });

        const data = await response.json();
        removeLoadingMessage();

        if (data.code === 200) {
            addMessage('ai', data.data);
            setStatus('apiStatusText', 'online', '正常');

            // 更新上下文计数
            const stats = await getStatsData();
            if (stats) {
                updateContextIndicator(stats.historySize || 0);
            }
        } else {
            addMessage('ai', `⚠️ 抱歉，处理您的请求时遇到了问题：${data.message || '未知错误'}`);
            setStatus('apiStatusText', 'offline', '异常');
        }
    } catch (error) {
        removeLoadingMessage();
        addMessage('ai', '🔌 网络连接异常，请检查网络后重试');
        setStatus('apiStatusText', 'offline', '连接失败');
        console.error('发送消息失败:', error);
    } finally {
        sendButton.disabled = false;
        messageInput.focus();
        // 重置输入框高度
        messageInput.style.height = 'auto';
    }
}

// 清除对话历史
async function clearHistory() {
    const userId = getUserId();
    if (!confirm('确定要清除对话历史吗？这将重置上下文记忆功能。')) return;

    try {
        const response = await fetch(`${API_BASE}/history/${userId}`, { method: 'DELETE' });
        const data = await response.json();
        alert(`✅ ${data.message || '对话历史已清除'}`);
        updateContextIndicator(0);
        setStatus('redisStatus', 'redisStatusText', 'online', '已清除');
    } catch (error) {
        alert(`❌ 清除失败: ${error.message}`);
    }
}

async function getHistory() {
    const userId = getUserId();
    try {
        const response = await fetch(`${API_BASE}/history/${userId}`);
        const history = await response.json();

        if (history.length === 0) {
            alert('📝 暂无对话历史\n\n上下文记忆为空，开始新的对话吧！');
            return;
        }

        let historyText = `📋 对话历史记录 (共${history.length}条)\n` + '='.repeat(40) + '\n\n';
        history.forEach((msg, index) => {
            const role = msg.role === 'user' ? '👤 用户' : '🤖 AI助手';
            const time = msg.timestamp ? new Date(msg.timestamp).toLocaleString() : '未知时间';
            historyText += `${index + 1}. [${role}] ${time}\n${msg.content}\n\n${'─'.repeat(30)}\n\n`;
        });

        alert(historyText);
    } catch (error) {
        alert(`❌ 获取历史失败: ${error.message}`);
    }
}

// 获取统计数据
async function getStatsData() {
    const userId = getUserId();
    try {
        const response = await fetch(`${API_BASE}/stats/${userId}`);
        return await response.json();
    } catch (error) {
        console.error('获取统计数据失败:', error);
        return null;
    }
}

// 获取详细统计
async function getStats() {
    const stats = await getStatsData();
    if (stats) {
        alert(
            `📊 用户对话统计信息\n` +
            '='.repeat(30) + `\n\n` +
            `👤 用户ID: ${stats.userId || getUserId()}\n` +
            `🧠 上下文记录数: ${stats.historySize || 0}\n` +
            `⏰ 统计时间: ${new Date(stats.timestamp || Date.now()).toLocaleString()}\n` +
            `💾 Redis 状态: 正常运行\n` +
            `🚀 Spring AI 状态: 服务在线`
        );
    } else {
        alert('❌ 无法获取统计信息，请检查后端服务连接');
    }
}

// 检查系统健康状态
async function checkSystemHealth() {
    try {
        const response = await fetch(`${API_BASE}/health`);
        const health = await response.json();

        if (health.status === 'UP') {
            setStatus('redisStatusText', 'online', '运行正常');
        } else {
            setStatus('redisStatusText', 'offline', '服务异常');
        }
    } catch (error) {
        setStatus('redisStatusText', 'offline', '连接失败');
    }
}

// 处理键盘事件
function handleKeyPress(event) {
    // Ctrl+Enter 发送消息
    if (event.key === 'Enter' && event.ctrlKey) {
        event.preventDefault();
        sendMessage();
        return;
    }

    // 自动调整输入框高度
    if (event.key === 'Enter' && !event.ctrlKey && !event.shiftKey) {
        event.preventDefault();
        // 允许按 Enter 发送，或者可以改为需要按 Ctrl+Enter
        // 这里我们改为按 Enter 发送，按 Shift+Enter 换行
        if (!event.shiftKey) {
            sendMessage();
        }
    }

    // 调整输入框高度
    const textarea = event.target;
    textarea.style.height = 'auto';
    textarea.style.height = (textarea.scrollHeight) + 'px';
}

// 页面加载完成后的初始化
document.addEventListener('DOMContentLoaded', function() {
    // 聚焦到输入框
    document.getElementById('messageInput').focus();

    // 检查系统健康状态
    checkSystemHealth();

    // 添加输入框事件监听
    const messageInput = document.getElementById('messageInput');
    messageInput.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    messageInput.addEventListener('keydown', handleKeyPress);

    // 监听用户ID变化，更新上下文统计
    document.getElementById('userId').addEventListener('change', async function() {
        const stats = await getStatsData();
        if (stats) {
            updateContextIndicator(stats.historySize || 0);
        } else {
            updateContextIndicator(0);
        }
    });

    // 定期检查系统状态（每30秒）
    setInterval(checkSystemHealth, 30000);

    // 初始获取上下文统计
    setTimeout(async () => {
        const stats = await getStatsData();
        if (stats) {
            updateContextIndicator(stats.historySize || 0);
        }
    }, 1000);
});