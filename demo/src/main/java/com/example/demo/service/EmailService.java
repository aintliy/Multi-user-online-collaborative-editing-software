package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * 发送简单文本邮件
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            System.out.println("邮件发送成功：" + to);
        } catch (Exception e) {
            System.err.println("邮件发送失败：" + e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }
    
    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "密码重置请求 - 多人在线协作编辑系统";
        String resetUrl = "http://localhost:3000/auth/reset-password?token=" + token;
        
        String text = String.format(
            "您好，\n\n" +
            "我们收到了您的密码重置请求。\n\n" +
            "请点击以下链接重置您的密码（链接有效期为1小时）：\n%s\n\n" +
            "如果您没有请求重置密码，请忽略此邮件。\n\n" +
            "此致\n" +
            "多人在线协作编辑系统团队",
            resetUrl
        );
        
        sendSimpleEmail(to, subject, text);
    }
    
    /**
     * 发送任务分配通知邮件
     */
    public void sendTaskAssignmentEmail(String to, String taskTitle, String documentTitle) {
        String subject = "新任务分配 - " + documentTitle;
        
        String text = String.format(
            "您好，\n\n" +
            "您在文档《%s》中被分配了一个新任务：\n\n" +
            "任务：%s\n\n" +
            "请登录系统查看详情。\n\n" +
            "此致\n" +
            "多人在线协作编辑系统团队",
            documentTitle,
            taskTitle
        );
        
        sendSimpleEmail(to, subject, text);
    }
}
