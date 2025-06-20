// --- DOM Elements ---
const connectWalletBtn = document.getElementById('connectWalletBtn');
const walletStatusEl = document.getElementById('walletStatus');
const walletAddressEl = document.getElementById('walletAddress');

const phoneNumberInput = document.getElementById('phoneNumber');
const sendSmsBtn = document.getElementById('sendSmsBtn');
const smsCodeInput = document.getElementById('smsCode');
const signupBtn = document.getElementById('signupBtn');
const signupMessageEl = document.getElementById('signupMessage');

const loginBtn = document.getElementById('loginBtn');
const loginMessageEl = document.getElementById('loginMessage');

const userInfoEl = document.getElementById('userInfo');
const userTokenEl = document.getElementById('userToken');
const userWalletAddressEl = document.getElementById('userWalletAddress');
const userRoleEl = document.getElementById('userRole');
const logoutBtn = document.getElementById('logoutBtn');

// --- State ---
let currentAccount = null;
let provider = null;
let signer = null;
const backendUrl = 'http://localhost:8080'; // 백엔드 주소

// --- Utility Functions ---

/**
 * Metamask 설치 여부 확인
 */
function isMetaMaskInstalled() {
    return typeof window.ethereum !== 'undefined';
}

/**
 * 메시지 표시 업데이트
 * @param {HTMLElement} element - 메시지를 표시할 요소
 * @param {string} message - 표시할 메시지
 * @param {boolean} isError - 에러 메시지 여부
 */
function updateMessage(element, message, isError = false) {
    element.textContent = message;
    element.className = 'message'; // Reset classes
    if (message) {
        element.classList.add(isError ? 'error' : 'success');
    }
}

/**
 * 버튼 활성화/비활성화
 * @param {HTMLButtonElement} button
 * @param {boolean} enabled
 */
function setButtonEnabled(button, enabled) {
    button.disabled = !enabled;
}

/**
 * API 호출 함수
 * @param {string} endpoint - API 엔드포인트 경로
 * @param {string} method - HTTP 메소드 ('GET', 'POST', 등)
 * @param {object} [body=null] - 요청 본문 (POST 요청 시)
 * @param {string} [token=null] - JWT 토큰 (인증 필요 시)
 * @returns {Promise<object>} - API 응답 데이터
 */
async function fetchApi(endpoint, method, body = null, token = null) {
    const headers = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method: method,
        headers: headers,
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(backendUrl + endpoint, options);
        if (!response.ok) {
            let errorData;
            try {
                errorData = await response.json();
            } catch (e) {
                errorData = { message: `HTTP error! Status: ${response.status}` };
            }
            console.error('API Error Response:', errorData);
            throw new Error(errorData.message || `HTTP error! Status: ${response.status}`);
        }
        // 성공 응답 본문이 없을 경우 빈 객체 반환 (e.g., 200 OK without body)
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return await response.json();
        } else {
            return {}; // 또는 필요시 null 반환
        }
    } catch (error) {
        console.error('Fetch API Error:', error);
        throw error; // 에러를 다시 던져서 호출한 곳에서 처리하도록 함
    }
}

/**
 * Metamask 서명 요청
 * @param {string} message - 서명할 메시지
 * @returns {Promise<string>} - 서명 값
 */
async function signMessage(message) {
    if (!signer) throw new Error('Wallet not connected or signer not available.');
    try {
        // personal_sign 사용 (이더리움 표준 서명)
        const signature = await signer.signMessage(message);
        console.log("Signature:", signature);
        return signature;
    } catch (error) {
        console.error("Failed to sign message:", error);
        throw new Error('Failed to sign message. User might have rejected.');
    }
}

// --- Wallet Connection ---

/**
 * Metamask 지갑 연결 및 상태 업데이트
 */
async function connectWallet() {
    if (!isMetaMaskInstalled()) {
        updateMessage(walletStatusEl, 'Metamask not installed!', true);
        return;
    }

    try {
        // Ethers.js provider 사용
        provider = new ethers.providers.Web3Provider(window.ethereum, "any");
        // 계정 요청
        const accounts = await provider.send("eth_requestAccounts", []);

        if (accounts.length > 0) {
            currentAccount = accounts[0];
            signer = provider.getSigner(); // 서명자 설정
            walletStatusEl.textContent = 'Connected';
            walletAddressEl.textContent = currentAccount;
            setButtonEnabled(connectWalletBtn, false); // 연결 후 비활성화
            setButtonEnabled(signupBtn, true); // 회원가입 버튼 활성화
            setButtonEnabled(loginBtn, true); // 로그인 버튼 활성화
        } else {
            handleAccountDisconnect();
        }
    } catch (error) {
        console.error("Failed to connect wallet:", error);
        walletStatusEl.textContent = 'Connection Failed';
        walletAddressEl.textContent = 'N/A';
        updateMessage(walletStatusEl, `Connection failed: ${error.message || 'Unknown error'}`, true);
    }
}

/**
 * 지갑 연결 해제 처리
 */
function handleAccountDisconnect() {
    currentAccount = null;
    signer = null;
    provider = null;
    walletStatusEl.textContent = 'Not Connected';
    walletAddressEl.textContent = 'N/A';
    setButtonEnabled(connectWalletBtn, true);
    setButtonEnabled(signupBtn, false);
    setButtonEnabled(loginBtn, false);
    clearUserInfo(); // 로그아웃 처리
    console.log("Wallet disconnected.");
}

// --- Signup Logic ---

/**
 * SMS 인증번호 발송 처리
 */
async function handleSendSms() {
    const phoneNumber = phoneNumberInput.value.trim();
    if (!phoneNumber) {
        updateMessage(signupMessageEl, 'Please enter phone number.', true);
        return;
    }
    // 간단한 전화번호 형식 검증 (자리수 등) - 필요시 추가
    if (!/^\d{10,11}$/.test(phoneNumber)) {
        updateMessage(signupMessageEl, 'Invalid phone number format.', true);
        return;
    }

    setButtonEnabled(sendSmsBtn, false); // 중복 클릭 방지
    updateMessage(signupMessageEl, 'Sending SMS code...');

    try {
        // 백엔드의 SMS 발송 엔드포인트 호출 (가정)
        await fetchApi('/api/sms/send', 'POST', { to: phoneNumber });
        updateMessage(signupMessageEl, 'SMS code sent. Please check your phone.', false);
    } catch (error) {
        updateMessage(signupMessageEl, `Failed to send SMS: ${error.message}`, true);
    } finally {
        setButtonEnabled(sendSmsBtn, true);
    }
}

/**
 * 회원가입 처리
 */
async function handleSignup() {
    if (!currentAccount) {
        updateMessage(signupMessageEl, 'Please connect your wallet first.', true);
        return;
    }
    const phoneNumber = phoneNumberInput.value.trim();
    const code = smsCodeInput.value.trim();

    if (!phoneNumber || !code) {
        updateMessage(signupMessageEl, 'Please enter phone number and SMS code.', true);
        return;
    }
    if (code.length !== 6 || !/^\d+$/.test(code)) {
        updateMessage(signupMessageEl, 'SMS code must be 6 digits.', true);
        return;
    }

    setButtonEnabled(signupBtn, false);
    updateMessage(signupMessageEl, 'Processing signup...');

    try {
        // 1. 서명할 메시지 생성 (백엔드와 동일하게)
        const messageToSign = `Register with: ${currentAccount}`;

        // 2. Metamask 서명 요청
        const signature = await signMessage(messageToSign);

        // 3. 백엔드 회원가입 API 호출
        const requestBody = {
            walletAddress: currentAccount,
            message: messageToSign,
            signature: signature,
            phoneNumber: phoneNumber,
            code: code
        };
        const result = await fetchApi('/api/user/register', 'POST', requestBody);

        updateMessage(signupMessageEl, result.message || 'Signup successful!', false);
        // 성공 시 입력 필드 초기화 등
        phoneNumberInput.value = '';
        smsCodeInput.value = '';

    } catch (error) {
        updateMessage(signupMessageEl, `Signup failed: ${error.message}`, true);
    } finally {
        setButtonEnabled(signupBtn, true);
    }
}

// --- Login Logic ---

/**
 * 로그인 처리
 */
async function handleLogin() {
    if (!currentAccount) {
        updateMessage(loginMessageEl, 'Please connect your wallet first.', true);
        return;
    }

    setButtonEnabled(loginBtn, false);
    updateMessage(loginMessageEl, 'Processing login...');

    try {
        // 1. 백엔드에서 Nonce 요청
        updateMessage(loginMessageEl, 'Requesting nonce...');
        const nonceResponse = await fetchApi(`/api/user/nonce?walletAddress=${currentAccount}`, 'GET');
        const nonce = nonceResponse.nonce;
        if (!nonce) {
            throw new Error('Failed to retrieve nonce from server.');
        }
        updateMessage(loginMessageEl, 'Nonce received. Please sign the message...');

        // 2. Nonce 값 서명 요청
        const signature = await signMessage(nonce); // Nonce가 서명할 메시지

        // 3. 백엔드 서명 검증 및 로그인 API 호출
        updateMessage(loginMessageEl, 'Verifying signature...');
        const requestBody = {
            walletAddress: currentAccount,
            message: nonce, // 서명한 Nonce를 message로 전송
            signature: signature
        };
        const loginResult = await fetchApi('/api/user/signature/verify', 'POST', requestBody);

        // 4. JWT 저장 및 사용자 정보 표시
        if (loginResult.token) {
            localStorage.setItem('jwtToken', loginResult.token); // LocalStorage에 JWT 저장
            localStorage.setItem('walletAddress', loginResult.walletAddress);
            localStorage.setItem('userRole', loginResult.role);
            updateMessage(loginMessageEl, 'Login successful!', false);
            displayUserInfo(loginResult.token, loginResult.walletAddress, loginResult.role);
        } else {
            throw new Error('Login failed: No token received.');
        }

    } catch (error) {
        updateMessage(loginMessageEl, `Login failed: ${error.message}`, true);
        clearUserInfo();
    } finally {
        setButtonEnabled(loginBtn, true);
    }
}

/**
 * 로그아웃 처리
 */
function handleLogout() {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('walletAddress');
    localStorage.removeItem('userRole');
    clearUserInfo();
    updateMessage(loginMessageEl, 'Logged out.');
    console.log("Logged out.");
}

/**
 * 로그인한 사용자 정보 표시
 */
function displayUserInfo(token, address, role) {
    userTokenEl.textContent = token.substring(0, 15) + '...'; // 너무 길면 축약
    userWalletAddressEl.textContent = address;
    userRoleEl.textContent = role;
    userInfoEl.style.display = 'block';
    setButtonEnabled(loginBtn, false); // 로그인 후 로그인 버튼 비활성화
}

/**
 * 사용자 정보 표시 영역 초기화
 */
function clearUserInfo() {
    userTokenEl.textContent = '';
    userWalletAddressEl.textContent = '';
    userRoleEl.textContent = '';
    userInfoEl.style.display = 'none';
    // 지갑이 연결되어 있다면 로그인 버튼 다시 활성화
    if (currentAccount) {
        setButtonEnabled(loginBtn, true);
    }
}

// --- Event Listeners ---
connectWalletBtn.addEventListener('click', connectWallet);
sendSmsBtn.addEventListener('click', handleSendSms);
signupBtn.addEventListener('click', handleSignup);
loginBtn.addEventListener('click', handleLogin);
logoutBtn.addEventListener('click', handleLogout);

// --- Initialization ---
window.addEventListener('load', () => {
    if (isMetaMaskInstalled()) {
        // Metamask 계정 변경 감지
        window.ethereum.on('accountsChanged', (accounts) => {
            console.log("Accounts changed:", accounts);
            if (accounts.length > 0) {
                // 연결된 계정으로 자동 업데이트
                connectWallet();
            } else {
                // 모든 계정 연결 해제 시
                handleAccountDisconnect();
            }
        });

        // Metamask 네트워크 변경 감지 (필요시)
        window.ethereum.on('chainChanged', (_chainId) => {
            console.log("Network changed:", _chainId);
            // 필요시 페이지 새로고침 또는 네트워크 관련 처리
            window.location.reload();
        });

        // 페이지 로드 시 기존 로그인 정보 확인
        const storedToken = localStorage.getItem('jwtToken');
        const storedAddress = localStorage.getItem('walletAddress');
        const storedRole = localStorage.getItem('userRole');
        if (storedToken && storedAddress && storedRole) {
            console.log("Found stored login info.");
            displayUserInfo(storedToken, storedAddress, storedRole);
            // 지갑도 자동으로 연결 시도 (사용자 경험 향상)
            connectWallet();
        } else {
            clearUserInfo();
        }


    } else {
        walletStatusEl.textContent = 'Metamask not installed!';
        setButtonEnabled(signupBtn, false);
        setButtonEnabled(loginBtn, false);
    }
});