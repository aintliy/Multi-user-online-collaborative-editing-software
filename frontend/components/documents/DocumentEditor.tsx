"use client";

import { Card } from "antd";
import Quill from "quill";
import "quill/dist/quill.snow.css";
import { useCallback, useEffect, useMemo, useRef } from "react";
import { EditOperation } from "@/types";

interface DocumentEditorProps {
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
  onRealtimeEdit?: (operation: EditOperation) => void;
}

const DocumentEditor = ({ value, onChange, readOnly, onRealtimeEdit }: DocumentEditorProps) => {
  const editorContainerRef = useRef<HTMLDivElement | null>(null);
  const quillRef = useRef<Quill | null>(null);

  const toolbar = useMemo(
    () => [
      [{ header: [1, 2, 3, false] }],
      ["bold", "italic", "underline", "strike"],
      [{ list: "ordered" }, { list: "bullet" }],
      [{ color: [] }, { background: [] }],
      [{ align: [] }],
      ["link", "blockquote", "code-block"],
      ["clean"],
    ],
    []
  );

  const emitChange = useCallback(() => {
    if (!quillRef.current) return;
    const html = quillRef.current.root.innerHTML;
    onChange(html);
    onRealtimeEdit?.({
      operation: "replace",
      index: 0,
      length: html.length,
      text: html,
    });
  }, [onChange, onRealtimeEdit]);

  useEffect(() => {
    if (!editorContainerRef.current || quillRef.current) {
      return;
    }

    const quillInstance = new Quill(editorContainerRef.current, {
      theme: "snow",
      modules: {
        toolbar,
      },
      readOnly,
    });

    quillRef.current = quillInstance;
    quillInstance.clipboard.dangerouslyPasteHTML(value || "", "silent");

    return () => {
      quillRef.current = null;
    };
  }, [readOnly, toolbar, value]);

  useEffect(() => {
    if (!quillRef.current) {
      return;
    }

    quillRef.current.on("text-change", emitChange);
    return () => {
      quillRef.current?.off("text-change", emitChange);
    };
  }, [emitChange]);

  useEffect(() => {
    if (!quillRef.current) return;
    quillRef.current.enable(!readOnly);
  }, [readOnly]);

  useEffect(() => {
    if (!quillRef.current) return;
    const html = quillRef.current.root.innerHTML;
    const nextValue = value || "";
    if (html === nextValue) {
      return;
    }

    const selection = quillRef.current.getSelection();
    quillRef.current.clipboard.dangerouslyPasteHTML(nextValue, "silent");
    if (selection) {
      quillRef.current.setSelection(selection);
    }
  }, [value]);

  return (
    <Card bordered={false} style={{ borderRadius: 18 }}>
      <div ref={editorContainerRef} />
    </Card>
  );
};

export default DocumentEditor;
