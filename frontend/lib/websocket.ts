import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface OnlineUser {
  userId: number;
  username: string;
  color: string;
}

export interface EditOperation {
  operation: 'INSERT' | 'DELETE' | 'REPLACE';
  position: number;
  content?: string;
  length?: number;
}

export interface CursorPosition {
  index: number;
  length: number;
  color?: string;
}

export interface WebSocketMessage {
  type: 'JOIN' | 'LEAVE' | 'EDIT' | 'CURSOR' | 'CHAT' | 'SAVE' | 'COMMENT' | 'ONLINE_USERS';
  docId: number;
  userId: number;
  username: string;
  payload: any;
}

export type MessageHandler = (message: WebSocketMessage) => void;

class WebSocketClient {
  private client: Client | null = null;
  private connected: boolean = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private messageHandlers: Map<string, MessageHandler[]> = new Map();
  private token: string | null = null;

  /**
   * 连接WebSocket服务器
   */
  connect(token: string): Promise<void> {
    this.token = token;

    return new Promise((resolve, reject) => {
      const socket = new SockJS(`${process.env.NEXT_PUBLIC_API_BASE_URL}/ws`);

      this.client = new Client({
        webSocketFactory: () => socket as any,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          console.log('[WebSocket]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected');
          this.connected = true;
          resolve();
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected');
          this.connected = false;
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          reject(new Error(frame.headers['message']));
        },
      });

      this.client.activate();
    });
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected = false;
      this.subscriptions.clear();
      this.messageHandlers.clear();
    }
  }

  /**
   * 订阅文档频道
   */
  subscribeDocument(docId: number, handler: MessageHandler): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    const topic = `/topic/document/${docId}`;
    
    // 添加消息处理器
    if (!this.messageHandlers.has(topic)) {
      this.messageHandlers.set(topic, []);
    }
    this.messageHandlers.get(topic)!.push(handler);

    // 如果已经订阅过，不重复订阅
    if (this.subscriptions.has(topic)) {
      return;
    }

    // 订阅主题
    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      const data: WebSocketMessage = JSON.parse(message.body);
      const handlers = this.messageHandlers.get(topic) || [];
      handlers.forEach((h) => h(data));
    });

    this.subscriptions.set(topic, subscription);

    // 发送订阅消息（触发后端的@SubscribeMapping）
    this.client.publish({
      destination: `/app/document/${docId}/subscribe`,
    });
  }

  /**
   * 取消订阅文档
   */
  unsubscribeDocument(docId: number): void {
    const topic = `/topic/document/${docId}`;
    
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(topic);
    }

    this.messageHandlers.delete(topic);

    // 发送离开消息
    if (this.client && this.connected) {
      this.client.publish({
        destination: `/app/document/${docId}/leave`,
      });
    }
  }

  /**
   * 订阅通知频道
   */
  subscribeNotifications(handler: MessageHandler): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    const topic = '/user/queue/notifications';
    
    // 添加消息处理器
    if (!this.messageHandlers.has(topic)) {
      this.messageHandlers.set(topic, []);
    }
    this.messageHandlers.get(topic)!.push(handler);

    // 如果已经订阅过，不重复订阅
    if (this.subscriptions.has(topic)) {
      return;
    }

    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      const data = JSON.parse(message.body);
      const handlers = this.messageHandlers.get(topic) || [];
      handlers.forEach((h) => h(data));
    });

    this.subscriptions.set(topic, subscription);
  }

  /**
   * 发送编辑操作
   */
  sendEdit(docId: number, operation: EditOperation): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/document/${docId}/edit`,
      body: JSON.stringify(operation),
    });
  }

  /**
   * 发送光标位置
   */
  sendCursor(docId: number, cursor: CursorPosition): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/document/${docId}/cursor`,
      body: JSON.stringify(cursor),
    });
  }

  /**
   * 发送聊天消息
   */
  sendChat(docId: number, content: string): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/document/${docId}/chat`,
      body: JSON.stringify(content),
    });
  }

  /**
   * 发送保存请求
   */
  sendSave(docId: number, content: string): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/document/${docId}/save`,
      body: JSON.stringify({ content }),
    });
  }

  /**
   * 请求在线用户列表
   */
  requestOnlineUsers(docId: number): void {
    if (!this.client || !this.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: `/app/document/${docId}/online-users`,
    });
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.connected;
  }
}

// 导出单例
export const websocketClient = new WebSocketClient();
