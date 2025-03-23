<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mfa-method'); section>
    <#if section = "header">
        ${msg("Pilih Metode Otentikasi Dua Faktor")}
    <#elseif section = "form">
        <form id="kc-select-mfa-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label class="${properties.kcLabelClass!}">${msg("Pilih faktor kedua Anda")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div>
                        <input type="radio" id="sms" name="mfa-method" value="sms" 
                            <#if sms_configured?? && sms_configured>checked</#if>>
                        <label for="sms">SMS <#if !sms_configured?? || !sms_configured>(${msg("Belum Dikonfigurasi")})</#if></label>
                    </div>
                    <div>
                        <input type="radio" id="whatsapp" name="mfa-method" value="whatsapp"
                            <#if whatsapp_configured?? && whatsapp_configured>checked</#if>>
                        <label for="whatsapp">WhatsApp <#if !whatsapp_configured?? || !whatsapp_configured>(${msg("Belum Dikonfigurasi")})</#if></label>
                    </div>
                    <div>
                        <input type="radio" id="telegram" name="mfa-method" value="telegram"
                            <#if telegram_configured?? && telegram_configured>checked</#if>>
                        <label for="telegram">Telegram <#if !telegram_configured?? || !telegram_configured>(${msg("Belum Dikonfigurasi")})</#if></label>
                    </div>
                    <div>
                        <input type="radio" id="email" name="mfa-method" value="email"
                            <#if email_configured?? && email_configured>checked</#if>>
                        <label for="email">Email <#if !email_configured?? || !email_configured>(${msg("Belum Dikonfigurasi")})</#if></label>
                    </div>
                    <div>
                        <input type="radio" id="totp" name="mfa-method" value="totp"
                            <#if totp_configured?? && totp_configured>checked</#if>>
                        <label for="totp">Aplikasi Autentikator <#if !totp_configured?? || !totp_configured>(${msg("Belum Dikonfigurasi")})</#if></label>
                    </div>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg('Lanjutkan')}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>