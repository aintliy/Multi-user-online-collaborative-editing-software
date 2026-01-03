import { notificationApi, collaboratorApi, friendApi } from '../api';

export interface HeartbeatData {
  unreadNotificationCount: number;
  pendingInvitesCount: number;
  pendingFriendRequestsCount: number;
}

type HeartbeatCallback = (data: HeartbeatData) => void;

class HeartbeatService {
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private callbacks: Set<HeartbeatCallback> = new Set();
  private lastData: HeartbeatData | null = null;
  private isRunning = false;
  
  // 心跳间隔（毫秒）
  private readonly HEARTBEAT_INTERVAL = 5000; // 5秒
  
  /**
   * 启动心跳服务
   */
  start() {
    if (this.isRunning) {
      return;
    }
    
    this.isRunning = true;
    
    // 立即执行一次
    this.pulse();
    
    // 设置定时器
    this.intervalId = setInterval(() => {
      this.pulse();
    }, this.HEARTBEAT_INTERVAL);
    
    console.log('[Heartbeat] Service started');
  }
  
  /**
   * 停止心跳服务
   */
  stop() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.isRunning = false;
    this.callbacks.clear();
    this.lastData = null;
    console.log('[Heartbeat] Service stopped');
  }
  
  /**
   * 订阅心跳数据更新
   */
  subscribe(callback: HeartbeatCallback): () => void {
    this.callbacks.add(callback);
    
    // 如果已有数据，立即回调
    if (this.lastData) {
      callback(this.lastData);
    }
    
    // 返回取消订阅函数
    return () => {
      this.callbacks.delete(callback);
    };
  }
  
  /**
   * 手动触发一次心跳（用于用户操作后立即刷新）
   */
  async refresh() {
    await this.pulse();
  }
  
  /**
   * 执行心跳请求
   */
  private async pulse() {
    try {
      // 并行请求所有数据
      const [notificationRes, invites, friendRequests] = await Promise.all([
        notificationApi.getUnreadCount().catch(() => ({ count: 0 })),
        collaboratorApi.getMyPendingInvites().catch(() => []),
        friendApi.getRequests().catch(() => []),
      ]);
      
      const data: HeartbeatData = {
        unreadNotificationCount: notificationRes.count,
        pendingInvitesCount: invites.length,
        pendingFriendRequestsCount: friendRequests.filter(r => r.status === 'PENDING').length,
      };
      
      this.lastData = data;
      
      // 通知所有订阅者
      this.callbacks.forEach(callback => {
        try {
          callback(data);
        } catch (e) {
          console.error('[Heartbeat] Callback error:', e);
        }
      });
      
    } catch (error) {
      console.error('[Heartbeat] Pulse failed:', error);
    }
  }
  
  /**
   * 获取最新数据
   */
  getLastData(): HeartbeatData | null {
    return this.lastData;
  }
}

// 单例导出
const heartbeatService = new HeartbeatService();
export default heartbeatService;
