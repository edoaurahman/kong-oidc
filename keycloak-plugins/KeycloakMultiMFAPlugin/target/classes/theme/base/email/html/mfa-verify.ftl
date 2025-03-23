<#import "./template.ftl" as layout>
<@layout.emailLayout>
    <div>
        <p>Hello ${username},</p>
        
        <p>Please verify your email address for ${realmName} by entering this verification code:</p>
        
        <div style="background-color: #f5f5f5; padding: 20px; margin: 20px 0; text-align: center; border-radius: 4px;">
            <span style="font-size: 24px; font-weight: bold; letter-spacing: 3px;">${code}</span>
        </div>
        
        <p>This code will expire in 5 minutes.</p>
        
        <p>If you did not request this verification, please ignore this email or contact support.</p>
    </div>
</@layout.emailLayout>