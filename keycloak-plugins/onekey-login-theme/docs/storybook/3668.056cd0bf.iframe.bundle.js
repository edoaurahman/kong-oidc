"use strict";(self.webpackChunkkeycloakify_starter=self.webpackChunkkeycloakify_starter||[]).push([[3668],{"./node_modules/keycloakify/login/pages/WebauthnRegister.js":(__unused_webpack_module,__webpack_exports__,__webpack_require__)=>{__webpack_require__.r(__webpack_exports__),__webpack_require__.d(__webpack_exports__,{default:()=>WebauthnRegister});var react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__=__webpack_require__("./node_modules/react/jsx-runtime.js"),react__WEBPACK_IMPORTED_MODULE_1__=__webpack_require__("./node_modules/react/index.js"),_tools_assert__WEBPACK_IMPORTED_MODULE_2__=__webpack_require__("./node_modules/keycloakify/tools/assert.js"),_login_lib_kcClsx__WEBPACK_IMPORTED_MODULE_3__=__webpack_require__("./node_modules/keycloakify/login/lib/kcClsx.js"),_tools_useInsertScriptTags__WEBPACK_IMPORTED_MODULE_4__=__webpack_require__("./node_modules/keycloakify/tools/useInsertScriptTags.js");function WebauthnRegister(props){const{kcContext,i18n,doUseDefaultCss,Template,classes}=props,{kcClsx}=(0,_login_lib_kcClsx__WEBPACK_IMPORTED_MODULE_3__.z)({doUseDefaultCss,classes}),{url,challenge,userid,username,signatureAlgorithms,rpEntityName,rpId,attestationConveyancePreference,authenticatorAttachment,requireResidentKey,userVerificationRequirement,createTimeout,excludeCredentialIds,isSetRetry,isAppInitiatedAction}=kcContext,{msg,msgStr}=i18n,{insertScriptTags}=(0,_tools_useInsertScriptTags__WEBPACK_IMPORTED_MODULE_4__.p)({componentOrHookName:"WebauthnRegister",scriptTags:[{type:"text/javascript",src:`${url.resourcesCommonPath}/node_modules/jquery/dist/jquery.min.js`},{type:"text/javascript",src:`${url.resourcesPath}/js/base64url.js`},{type:"text/javascript",textContent:`\n                function registerSecurityKey() {\n\n                    // Check if WebAuthn is supported by this browser\n                    if (!window.PublicKeyCredential) {\n                        $("#error").val("${msgStr("webauthn-unsupported-browser-text")}");\n                        $("#register").submit();\n                        return;\n                    }\n    \n                    // mandatory parameters\n                    let challenge = "${challenge}";\n                    let userid = "${userid}";\n                    let username = "${username}";\n    \n                    let signatureAlgorithms =${JSON.stringify(signatureAlgorithms)};\n                    let pubKeyCredParams = getPubKeyCredParams(signatureAlgorithms);\n    \n                    let rpEntityName = "${rpEntityName}";\n                    let rp = {name: rpEntityName};\n    \n                    let publicKey = {\n                        challenge: base64url.decode(challenge, {loose: true}),\n                        rp: rp,\n                        user: {\n                            id: base64url.decode(userid, {loose: true}),\n                            name: username,\n                            displayName: username\n                        },\n                        pubKeyCredParams: pubKeyCredParams,\n                    };\n    \n                    // optional parameters\n                    let rpId = "${rpId}";\n                    publicKey.rp.id = rpId;\n    \n                    let attestationConveyancePreference = "${attestationConveyancePreference}";\n                    if (attestationConveyancePreference !== 'not specified') publicKey.attestation = attestationConveyancePreference;\n    \n                    let authenticatorSelection = {};\n                    let isAuthenticatorSelectionSpecified = false;\n    \n                    let authenticatorAttachment = "${authenticatorAttachment}";\n                    if (authenticatorAttachment !== 'not specified') {\n                        authenticatorSelection.authenticatorAttachment = authenticatorAttachment;\n                        isAuthenticatorSelectionSpecified = true;\n                    }\n    \n                    let requireResidentKey = "${requireResidentKey}";\n                    if (requireResidentKey !== 'not specified') {\n                        if (requireResidentKey === 'Yes')\n                            authenticatorSelection.requireResidentKey = true;\n                        else\n                            authenticatorSelection.requireResidentKey = false;\n                        isAuthenticatorSelectionSpecified = true;\n                    }\n    \n                    let userVerificationRequirement = "${userVerificationRequirement}";\n                    if (userVerificationRequirement !== 'not specified') {\n                        authenticatorSelection.userVerification = userVerificationRequirement;\n                        isAuthenticatorSelectionSpecified = true;\n                    }\n    \n                    if (isAuthenticatorSelectionSpecified) publicKey.authenticatorSelection = authenticatorSelection;\n    \n                    let createTimeout = ${createTimeout};\n                    if (createTimeout !== 0) publicKey.timeout = createTimeout * 1000;\n    \n                    let excludeCredentialIds = "${excludeCredentialIds}";\n                    let excludeCredentials = getExcludeCredentials(excludeCredentialIds);\n                    if (excludeCredentials.length > 0) publicKey.excludeCredentials = excludeCredentials;\n    \n                    navigator.credentials.create({publicKey})\n                        .then(function (result) {\n                            window.result = result;\n                            let clientDataJSON = result.response.clientDataJSON;\n                            let attestationObject = result.response.attestationObject;\n                            let publicKeyCredentialId = result.rawId;\n    \n                            $("#clientDataJSON").val(base64url.encode(new Uint8Array(clientDataJSON), {pad: false}));\n                            $("#attestationObject").val(base64url.encode(new Uint8Array(attestationObject), {pad: false}));\n                            $("#publicKeyCredentialId").val(base64url.encode(new Uint8Array(publicKeyCredentialId), {pad: false}));\n    \n                            if (typeof result.response.getTransports === "function") {\n                                let transports = result.response.getTransports();\n                                if (transports) {\n                                    $("#transports").val(getTransportsAsString(transports));\n                                }\n                            } else {\n                                console.log("Your browser is not able to recognize supported transport media for the authenticator.");\n                            }\n    \n                            let initLabel = "WebAuthn Authenticator (Default Label)";\n                            let labelResult = window.prompt("Please input your registered authenticator's label", initLabel);\n                            if (labelResult === null) labelResult = initLabel;\n                            $("#authenticatorLabel").val(labelResult);\n    \n                            $("#register").submit();\n    \n                        })\n                        .catch(function (err) {\n                            $("#error").val(err);\n                            $("#register").submit();\n    \n                        });\n                }\n    \n                function getPubKeyCredParams(signatureAlgorithmsList) {\n                    let pubKeyCredParams = [];\n                    if (signatureAlgorithmsList.length === 0) {\n                        pubKeyCredParams.push({type: "public-key", alg: -7});\n                        return pubKeyCredParams;\n                    }\n    \n                    for (let i = 0; i < signatureAlgorithmsList.length; i++) {\n                        pubKeyCredParams.push({\n                            type: "public-key",\n                            alg: signatureAlgorithmsList[i]\n                        });\n                    }\n                    return pubKeyCredParams;\n                }\n    \n                function getExcludeCredentials(excludeCredentialIds) {\n                    let excludeCredentials = [];\n                    if (excludeCredentialIds === "") return excludeCredentials;\n    \n                    let excludeCredentialIdsList = excludeCredentialIds.split(',');\n    \n                    for (let i = 0; i < excludeCredentialIdsList.length; i++) {\n                        excludeCredentials.push({\n                            type: "public-key",\n                            id: base64url.decode(excludeCredentialIdsList[i],\n                            {loose: true})\n                        });\n                    }\n                    return excludeCredentials;\n                }\n    \n                function getTransportsAsString(transportsList) {\n                    if (transportsList === '' || Array.isArray(transportsList)) return "";\n    \n                    let transportsString = "";\n    \n                    for (let i = 0; i < transportsList.length; i++) {\n                        transportsString += transportsList[i] + ",";\n                    }\n    \n                    return transportsString.slice(0, -1);\n                }\n                `}]});return(0,react__WEBPACK_IMPORTED_MODULE_1__.useEffect)((()=>{insertScriptTags()}),[]),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsxs)(Template,Object.assign({kcContext,i18n,doUseDefaultCss,classes,headerNode:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsxs)(react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.Fragment,{children:[(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("span",{className:kcClsx("kcWebAuthnKeyIcon")}),msg("webauthn-registration-title")]})},{children:[(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("form",Object.assign({id:"register",className:kcClsx("kcFormClass"),action:url.loginAction,method:"post"},{children:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsxs)("div",Object.assign({className:kcClsx("kcFormGroupClass")},{children:[(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"clientDataJSON",name:"clientDataJSON"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"attestationObject",name:"attestationObject"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"publicKeyCredentialId",name:"publicKeyCredentialId"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"authenticatorLabel",name:"authenticatorLabel"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"transports",name:"transports"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"hidden",id:"error",name:"error"}),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)(LogoutOtherSessions,{kcClsx,i18n})]}))})),(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"submit",className:kcClsx("kcButtonClass","kcButtonPrimaryClass","kcButtonBlockClass","kcButtonLargeClass"),id:"registerWebAuthn",value:msgStr("doRegisterSecurityKey"),onClick:()=>{(0,_tools_assert__WEBPACK_IMPORTED_MODULE_2__.v)("registerSecurityKey"in window),(0,_tools_assert__WEBPACK_IMPORTED_MODULE_2__.v)("function"==typeof window.registerSecurityKey),window.registerSecurityKey()}}),!isSetRetry&&isAppInitiatedAction&&(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("form",Object.assign({action:url.loginAction,className:kcClsx("kcFormClass"),id:"kc-webauthn-settings-form",method:"post"},{children:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("button",Object.assign({type:"submit",className:kcClsx("kcButtonClass","kcButtonDefaultClass","kcButtonBlockClass","kcButtonLargeClass"),id:"cancelWebAuthnAIA",name:"cancel-aia",value:"true"},{children:msg("doCancel")}))}))]}))}function LogoutOtherSessions(props){const{kcClsx,i18n}=props,{msg}=i18n;return(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("div",Object.assign({id:"kc-form-options",className:kcClsx("kcFormOptionsClass")},{children:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("div",Object.assign({className:kcClsx("kcFormOptionsWrapperClass")},{children:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("div",Object.assign({className:"checkbox"},{children:(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsxs)("label",{children:[(0,react_jsx_runtime__WEBPACK_IMPORTED_MODULE_0__.jsx)("input",{type:"checkbox",id:"logout-sessions",name:"logout-sessions",value:"on",defaultChecked:!0}),msg("logoutOtherSessions")]})}))}))}))}}}]);