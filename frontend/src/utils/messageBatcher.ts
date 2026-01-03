import { message } from 'antd';

type MessageType = 'info' | 'success' | 'warning' | 'error';

interface BatchedMessage {
  type: MessageType;
  messages: string[];
  timer: ReturnType<typeof setTimeout> | null;
}

/**
 * 消息批处理器
 * 将短时间内相同类型的消息合并后一次性显示，减少消息弹窗数量
 */
class MessageBatcher {
  private batches: Map<string, BatchedMessage> = new Map();
  private batchDelay = 100; // 合并延迟时间 (ms)，从 300ms 优化到 100ms

  /**
   * 添加一条消息到批处理队列
   * @param type 消息类型
   * @param content 消息内容
   * @param category 消息分类（相同分类的消息会被合并）
   */
  add(type: MessageType, content: string, category: string = 'default') {
    const key = `${type}:${category}`;
    let batch = this.batches.get(key);

    if (!batch) {
      batch = {
        type,
        messages: [],
        timer: null,
      };
      this.batches.set(key, batch);
    }

    // 避免重复消息
    if (!batch.messages.includes(content)) {
      batch.messages.push(content);
    }

    // 重置定时器
    if (batch.timer) {
      clearTimeout(batch.timer);
    }

    batch.timer = setTimeout(() => {
      // 使用 requestAnimationFrame 确保在渲染帧中显示，避免卡顿
      requestAnimationFrame(() => {
        this.flush(key);
      });
    }, this.batchDelay);
  }

  /**
   * 立即显示指定分类的所有消息
   */
  private flush(key: string) {
    const batch = this.batches.get(key);
    if (!batch || batch.messages.length === 0) {
      return;
    }

    const { type, messages } = batch;
    
    // 合并消息
    let displayContent: string;
    if (messages.length === 1) {
      displayContent = messages[0];
    } else if (messages.length <= 3) {
      // 少于等于3条，直接列出
      displayContent = messages.join('、');
    } else {
      // 超过3条，显示前2条 + 数量
      displayContent = `${messages.slice(0, 2).join('、')} 等 ${messages.length} 条消息`;
    }

    // 显示消息
    message[type](displayContent);

    // 清理
    this.batches.delete(key);
  }

  /**
   * 立即显示所有待处理的消息
   */
  flushAll() {
    this.batches.forEach((batch, key) => {
      if (batch.timer) {
        clearTimeout(batch.timer);
      }
      this.flush(key);
    });
  }

  /**
   * 清除所有待处理的消息（不显示）
   */
  clear() {
    this.batches.forEach((batch) => {
      if (batch.timer) {
        clearTimeout(batch.timer);
      }
    });
    this.batches.clear();
  }

  // 便捷方法
  info(content: string, category: string = 'default') {
    this.add('info', content, category);
  }

  success(content: string, category: string = 'default') {
    this.add('success', content, category);
  }

  warning(content: string, category: string = 'default') {
    this.add('warning', content, category);
  }

  error(content: string, category: string = 'default') {
    this.add('error', content, category);
  }
}

export const messageBatcher = new MessageBatcher();
export default messageBatcher;
