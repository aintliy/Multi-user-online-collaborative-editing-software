import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { WebSocketMessage, CursorPosition, DocumentOperation } from '../types';

class WebSocketService {
  private client: Client | null = null;
  private documentId: number | null = null;
  private subscriptions: Map<string, any> = new Map();
  private messageHandlers: Map<string, (message: WebSocketMessage) => void> = new Map();

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
          console.log('[STOMP]', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected');
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          reject(new Error(frame.body));
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected');
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      this.documentId = null;
    }
  }

  joinDocument(documentId: number): void {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket not connected');
      return;
    }

    this.documentId = documentId;

    // 订阅文档频道
    const subscription = this.client.subscribe(
      `/topic/document/${documentId}`,
      (message: IMessage) => {
        const data: WebSocketMessage = JSON.parse(message.body);
        this.handleMessage(data);
      }
    );
    this.subscriptions.set(`document-${documentId}`, subscription);

    // 发送加入消息
    this.client.publish({
      destination: `/app/document/${documentId}/join`,
    });
  }

  leaveDocument(): void {
    if (!this.client || !this.client.connected || !this.documentId) {
      return;
    }

    // 发送离开消息
    this.client.publish({
      destination: `/app/document/${this.documentId}/leave`,
    });

    // 取消订阅
    const subscription = this.subscriptions.get(`document-${this.documentId}`);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(`document-${this.documentId}`);
    }

    this.documentId = null;
  }

  sendEdit(operation: DocumentOperation): void {
    if (!this.client || !this.client.connected || !this.documentId) {
      return;
    }

    this.client.publish({
      destination: `/app/document/${this.documentId}/edit`,
      body: JSON.stringify(operation),
    });
  }

  sendCursor(cursor: CursorPosition): void {
    if (!this.client || !this.client.connected || !this.documentId) {
      return;
    }

    this.client.publish({
      destination: `/app/document/${this.documentId}/cursor`,
      body: JSON.stringify(cursor),
    });
  }

  sendChatMessage(content: string): void {
    if (!this.client || !this.client.connected || !this.documentId) {
      return;
    }

    this.client.publish({
      destination: `/app/document/${this.documentId}/chat`,
      body: JSON.stringify({ content }),
    });
  }

  onMessage(type: string, handler: (message: WebSocketMessage) => void): void {
    this.messageHandlers.set(type, handler);
  }

  offMessage(type: string): void {
    this.messageHandlers.delete(type);
  }

  private handleMessage(message: WebSocketMessage): void {
    const handler = this.messageHandlers.get(message.type);
    if (handler) {
      handler(message);
    }

    // 通用处理器
    const allHandler = this.messageHandlers.get('*');
    if (allHandler) {
      allHandler(message);
    }
  }

  subscribeToNotifications(_userId: number, handler: (message: any) => void): void {
    if (!this.client || !this.client.connected) {
      return;
    }

    const subscription = this.client.subscribe(
      `/user/queue/notifications`,
      (message: IMessage) => {
        handler(JSON.parse(message.body));
      }
    );
    this.subscriptions.set('notifications', subscription);
  }

  get isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  get currentDocumentId(): number | null {
    return this.documentId;
  }
}

export const wsService = new WebSocketService();
export default wsService;
