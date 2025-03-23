<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("verifyYourEmail")}
    <#elseif section = "form">
        <form id="kc-email-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">
                        ${msg("emailVerificationCode")}
                    </label>
                </div>

                <div class="form-info-text" style="margin-bottom: 15px;">
                    ${msg("emailVerificationInstruction", email!'')}
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="code" name="code" class="${properties.kcInputClass!}"
                           inputmode="numeric"
                           autocomplete="one-time-code"
                           pattern="[0-9]*"
                           minlength="6"
                           maxlength="6"
                           autofocus
                           required/>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("verify")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>