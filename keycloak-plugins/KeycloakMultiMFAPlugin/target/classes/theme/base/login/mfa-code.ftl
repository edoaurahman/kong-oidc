<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("Enter Verification Code")}
    <#elseif section = "form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">
                        <#if method?? && method == "totp">
                            ${msg("Enter code from your authenticator app")}
                        <#else>
                            ${msg("Enter the code we sent you")}
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
                           type="submit" value="${msg('Submit')}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>