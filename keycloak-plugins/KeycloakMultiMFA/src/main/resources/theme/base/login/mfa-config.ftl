<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mfa-config'); section>
    <#if section = "header">
        ${msg("Konfigurasi")?replace("{0}", (method!'')?capitalize)}
    <#elseif section = "form">
        <form id="kc-mfa-config-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" id="mfa-method" name="mfa-method" value="${method!''}">
            
            <#if method?? && method == "sms">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="PhoneNumber" class="${properties.kcLabelClass!}">${msg("Nomor Telepon")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="tel" id="PhoneNumber" name="PhoneNumber" class="${properties.kcInputClass!}"
                               placeholder="0812******** atau +62812********"
                               value="${(PhoneNumber!'')}"/>
                        <div class="phone-instructions" style="margin-top: 10px; padding: 10px; border-radius: 4px; background-color: #f8f9fa;">
                            <p><small>Masukkan nomor telepon dengan format 081xxx atau +6281xxx<br>
                            Nomor yang dimulai dengan 0 akan otomatis diformat menjadi +62xxx</small></p>
                        </div>
                    </div>
                </div>
            <#elseif method?? && method == "whatsapp">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="PhoneNumber" class="${properties.kcLabelClass!}">${msg("Nomor WhatsApp")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="tel" id="PhoneNumber" name="PhoneNumber" class="${properties.kcInputClass!}"
                            placeholder="0812******** atau +62812********"
                            value="${(PhoneNumber!'')}"/>
                        <div class="whatsapp-instructions" style="margin-top: 10px; padding: 10px; border-radius: 4px; background-color: #f8f9fa;">
                            <p><small>
                            Masukkan nomor WhatsApp dengan format:
                            <ul style="margin-top: 5px; margin-bottom: 5px; padding-left: 20px;">
                                <li>081xxx (akan diformat otomatis menjadi +6281xxx)</li>
                                <li>+6281xxx (nomor dengan kode negara Indonesia)</li>
                            </ul>
                            <br>
                            Setelah mengklik "Lanjutkan", kode verifikasi akan dikirim ke WhatsApp Anda.
                            </small></p>
                        </div>
                    </div>
                </div> 
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                            type="submit" value="${msg('Lanjutkan')}"/>
                    </div>
                </div>
         
            <#elseif method?? && method == "telegram">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="telegramId" class="${properties.kcLabelClass!}">${msg("ID Chat Telegram")}</label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <input type="text" id="telegramId" name="telegramId" class="${properties.kcInputClass!}"
                               placeholder="${msg('Masukkan ID Telegram Anda')}"
                               value="${(telegramId!'')}"/>
                        <div class="telegram-instructions" style="margin-top: 10px; padding: 10px; border-radius: 4px; background-color: #f8f9fa;">
                            <p><strong>${msg("Untuk mendapatkan ID Chat Telegram Anda")}:</strong></p>
                            <ol style="margin-left: 20px;">
                                <li>Buka Telegram dan cari bot kami: <strong>${msg("telegrambotusername")}</strong></li>
                                <li>Kirim pesan apapun ke bot dan bot akan membalas dengan ID Chat Anda</li>
                                <li>Salin ID Chat dan tempelkan di sini</li>
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
                               placeholder="${msg('Masukkan alamat email Anda')}"
                               value="${(email!'')}"/>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg('Kirim')}"/>
                </div>
            </div>
        </form>
        
        <#if method?? && (method == "whatsapp" || method == "sms")>
        <script>
            // Script untuk memformat nomor telepon Indonesia saat input
            document.addEventListener('DOMContentLoaded', function() {
                // Pilih input field berdasarkan metode
                var phoneInput = document.getElementById(method == "whatsapp" ? "PhoneNumber" : "PhoneNumber");
                
                if (phoneInput) {
                    // Format input saat diubah
                    phoneInput.addEventListener('input', function(e) {
                        var value = e.target.value.trim();
                        
                        // Hapus semua karakter non-angka kecuali tanda +
                        value = value.replace(/[^\d+]/g, '');
                        
                        // Jika dimulai dengan 0, ganti dengan +62
                        if (value.startsWith('0')) {
                            value = '+62' + value.substring(1);
                        }
                        
                        // Batasi panjang input (misalnya max 15 karakter)
                        if (value.length > 15) {
                            value = value.substring(0, 15);
                        }
                        
                        e.target.value = value;
                    });
                    
                    // Format input saat halaman dimuat (jika sudah ada nilai)
                    if (phoneInput.value && phoneInput.value.startsWith('0')) {
                        phoneInput.value = '+62' + phoneInput.value.substring(1);
                    }
                }
            });
        </script>
        </#if>
    </#if>
</@layout.registrationLayout>