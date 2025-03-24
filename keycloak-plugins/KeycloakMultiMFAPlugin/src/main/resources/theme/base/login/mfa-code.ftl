<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("Masukkan Kode Verifikasi")}
    <#elseif section = "form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">
                        <#if method?? && method == "totp">
                            ${msg("Masukkan kode dari aplikasi autentikator Anda")}
                        <#elseif method?? && method == "whatsapp">
                            ${msg("Masukkan kode yang kami kirim ke WhatsApp Anda")}
                        <#else>
                            ${msg("Masukkan kode yang telah kami kirimkan")}
                        </#if>
                    </label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="code" name="code" type="text" class="${properties.kcInputClass!}"
                           pattern="[0-9]*" 
                           inputmode="numeric"
                           autocomplete="one-time-code"
                           minlength="6" maxlength="6"
                           autofocus required/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg('Verifikasi')}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>