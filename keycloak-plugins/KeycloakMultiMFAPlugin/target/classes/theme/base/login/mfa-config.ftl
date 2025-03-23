<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mfa-config'); section>
    <#if section = "header">
        ${msg("Configure")?replace("{0}", (method!'')?capitalize)}
    <#elseif section = "form">
        <form id="kc-mfa-config-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" id="mfa-method" name="mfa-method" value="${method!''}">
            
            <#if method?? && method == "sms">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="phoneNumber" class="${properties.kcLabelClass!}">${msg("Phone Number")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="tel" id="phoneNumber" name="phoneNumber" class="${properties.kcInputClass!}"
                               placeholder="+1234567890"
                               value="${(phoneNumber!'')}"/>
                    </div>
                </div>
            <#elseif method?? && method == "telegram">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="telegramId" class="${properties.kcLabelClass!}">${msg("Telegram Chat ID")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="text" id="telegramId" name="telegramId" class="${properties.kcInputClass!}"
                               placeholder="${msg('Enter your telegram id')}"
                               value="${(telegramId!'')}"/>
                        <div class="telegram-instructions" style="margin-top: 10px; padding: 10px; border-radius: 4px;">
                            <p><strong>${msg("To get your Telegram Chat ID")}:</strong></p>
                            <ol style="margin-left: 20px;">
                                <li>Open Telegram and search for our bot: <strong>${msg("telegrambotusername")}</strong></li>
                                <li>Message the bot anything and it will reply with your Chat ID</li>
                                <li>Copy the Chat ID and paste it here</li>
                            </ol>
                        </div>
                    </div>
                </div>
            <#elseif method?? && method == "email">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="email" class="${properties.kcLabelClass!}">${msg("Email")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="email" id="email" name="email" class="${properties.kcInputClass!}"
                               placeholder="${msg('Enter your email address')}"
                               value="${(email!'')}"/>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg('Submit')}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>