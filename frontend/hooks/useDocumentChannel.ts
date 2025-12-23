"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getApiBaseUrl } from "@/lib/apiClient";
import { ChatMessage, CursorPosition, EditOperation, OnlineUser } from "@/types";
import { useAuth } from "./useAuth";

interface DocumentChannelResult {
  onlineUsers: OnlineUser[];
  chatMessages: ChatMessage[];
  connected: boolean;
  sendChat: (content: string) => void;
  sendEdit: (operation: EditOperation) => void;
  sendCursor: (cursor: CursorPosition) => void;
}

export const useDocumentChannel = (docId: number | null): DocumentChannelResult => {
  const { token } = useAuth();
  const [connected, setConnected] = useState(false);
  const [onlineUsers, setOnlineUsers] = useState<OnlineUser[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!docId || !token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${getApiBaseUrl()}/ws`) as unknown as WebSocket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      debug: () => undefined,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/document/${docId}`, (message) => handlePayload(message));
        client.publish({ destination: `/app/document/${docId}/online-users`, body: "" });
      },
      onDisconnect: () => {
        setConnected(false);
      },
    });

    const handlePayload = (message: IMessage) => {
      try {
        const body = JSON.parse(message.body);
        const type = body.type;
        if (type === "JOIN" || type === "LEAVE" || type === "ONLINE_USERS") {
          if (Array.isArray(body.payload)) {
            setOnlineUsers(body.payload);
          }
          if (type !== "ONLINE_USERS") {
            setChatMessages((prev) => [
              {
                id: `${Date.now()}`,
                userId: body.userId,
                username: body.username,
                content: type === "JOIN" ? `${body.username} 加入文档` : `${body.username} 离开文档`,
                timestamp: Date.now(),
                type: "SYSTEM",
              },
              ...prev,
            ]);
          }
        }
        if (type === "CHAT") {
          setChatMessages((prev) => [
            ...prev,
            {
              id: `${body.docId}-${body.timestamp}`,
              userId: body.userId,
              username: body.username,
              content: body.payload ?? body.content,
              timestamp: body.timestamp || Date.now(),
              type: "CHAT",
            },
          ]);
        }
      } catch (error) {
        console.warn("Failed to parse socket payload", error);
      }
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (client.connected) {
        client.publish({ destination: `/app/document/${docId}/leave`, body: "" });
      }
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [docId, token]);

  const publish = useCallback(
    (destination: string, body: object | string) => {
      if (!clientRef.current || !clientRef.current.connected) return;
      clientRef.current.publish({
        destination,
        body: typeof body === "string" ? body : JSON.stringify(body),
      });
    },
    []
  );

  const sendChat = useCallback(
    (content: string) => {
      if (!docId || !content.trim()) return;
      publish(`/app/document/${docId}/chat`, content.trim());
    },
    [docId, publish]
  );

  const sendEdit = useCallback(
    (operation: EditOperation) => {
      if (!docId) return;
      publish(`/app/document/${docId}/edit`, operation);
    },
    [docId, publish]
  );

  const sendCursor = useCallback(
    (cursor: CursorPosition) => {
      if (!docId) return;
      publish(`/app/document/${docId}/cursor`, cursor);
    },
    [docId, publish]
  );

  return {
    onlineUsers,
    chatMessages,
    connected,
    sendChat,
    sendEdit,
    sendCursor,
  };
};
