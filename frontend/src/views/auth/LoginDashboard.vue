<script setup>
import FloatingConfigurator from '@/components/FloatingConfigurator.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AuthService from '@/apis/auth'
import { useToast } from 'primevue/usetoast'
import Toast from 'primevue/toast'

const router = useRouter()
const toast = useToast()

const email = ref('')
const password = ref('')
const checked = ref(false)
const isLoading = ref(false)

onMounted(() => {
  // Check for session expiration message
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('expired') === 'true') {
    toast.add({
      severity: 'warn',
      summary: 'Phiên đăng nhập hết hạn',
      detail: 'Vui lòng đăng nhập lại để tiếp tục',
      life: 5000,
    })
  }

  // Check if user is already authenticated
  if (AuthService.isAuthenticated()) {
    router.push('/')
  }
})

const isValidEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(email)
}

const handleLogin = async () => {
  if (!email.value || !password.value) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Vui lòng nhập đầy đủ email và mật khẩu',
      life: 3000,
    })
    return
  }

  if (!isValidEmail(email.value)) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Email không đúng định dạng',
      life: 3000,
    })
    return
  }

  if (password.value.length < 6) {
    toast.add({
      severity: 'warn',
      summary: 'Cảnh báo',
      detail: 'Mật khẩu phải có ít nhất 6 ký tự',
      life: 3000,
    })
    return
  }

  try {
    isLoading.value = true
    await AuthService.login(email.value, password.value)

    // Hiển thị thông báo thành công
    toast.add({
      severity: 'success',
      summary: 'Thành công',
      detail: 'Đăng nhập thành công',
      life: 3000,
    })

    // Clear any expired session parameters from URL
    const url = new URL(window.location)
    url.searchParams.delete('expired')
    window.history.replaceState({}, '', url)

    router.push('/')
  } catch (error) {
    console.error('Login error:', error)
    console.error('Error response:', error.response)

    let errorMessage = 'Email hoặc mật khẩu không đúng. Vui lòng kiểm tra lại.'
    let errorSummary = 'Lỗi đăng nhập'

    // Handle specific error codes from backend
    if (error.response?.data?.code) {
      const errorCode = error.response.data.code
      switch (errorCode) {
        case 'TOKEN_EXPIRED':
        case 'TOKEN_INVALID':
          errorMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
          errorSummary = 'Phiên hết hạn'
          break
        case 'ACCOUNT_INACTIVE':
          errorMessage = 'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.'
          errorSummary = 'Tài khoản bị khóa'
          break
        default:
          errorMessage = error.response.data.message || errorMessage
      }
    } else if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    }

    // Hiển thị thông báo lỗi
    toast.add({
      severity: 'error',
      summary: errorSummary,
      detail: errorMessage,
      life: 5000,
    })
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <Toast />

  <FloatingConfigurator />
  <div
    class="bg-surface-50 dark:bg-surface-950 flex items-center justify-center min-h-screen min-w-[100vw] overflow-hidden"
  >
    <div class="flex flex-col items-center justify-center">
      <div
        style="
          border-radius: 56px;
          padding: 0.3rem;
          background: linear-gradient(180deg, var(--primary-color) 10%, rgba(33, 150, 243, 0) 30%);
        "
      >
        <div
          class="w-full bg-surface-0 dark:bg-surface-900 py-20 px-8 sm:px-20"
          style="border-radius: 53px"
        >
          <div class="text-center mb-8">
            <svg
              class="mb-8 w-20 h-20 shrink-0 mx-auto"
              version="1.0"
              xmlns="http://www.w3.org/2000/svg"
              width="1000.000000pt"
              height="1000.000000pt"
              viewBox="0 0 1000.000000 1000.000000"
              preserveAspectRatio="xMidYMid meet"
            >
              <g
                transform="translate(0.000000,1000.000000) scale(0.100000,-0.100000)"
                fill="#000000"
                stroke="none"
              >
                <path
                  fill="var(--primary-color)"
                  d="M4830 9689 c-495 -37 -940 -138 -1390 -316 -185 -73 -440 -194 -574
-271 -84 -49 -91 -52 -141 -46 -95 11 -160 -29 -186 -115 -18 -60 -4 -109 45
-157 66 -67 148 -72 219 -13 25 21 41 45 50 77 12 41 21 51 78 86 82 51 321
169 471 235 251 108 659 231 935 280 452 81 938 93 1387 36 108 -14 109 -15
142 -54 97 -113 277 -60 291 86 9 92 -53 171 -143 181 -34 3 -56 -1 -100 -23
-55 -27 -58 -27 -143 -16 -276 37 -673 50 -941 30z"
                />
                <path
                  fill="var(--primary-color)"
                  d="M6875 9391 c-121 -62 -120 -232 1 -291 45 -22 93 -25 138 -9 28 10
40 7 116 -29 247 -118 574 -328 825 -528 755 -604 1300 -1451 1535 -2384 58
-229 120 -594 120 -706 0 -31 -3 -34 -29 -34 -85 0 -228 -96 -272 -182 l-19
-38 -269 0 -268 0 -7 108 c-22 336 -105 710 -231 1037 l-56 144 25 38 c39 59
57 122 57 199 0 152 -75 272 -211 338 -59 28 -74 31 -160 31 -81 -1 -103 -5
-152 -27 -150 -70 -241 -245 -208 -404 34 -165 161 -284 320 -301 l62 -6 24
-54 c104 -237 209 -671 229 -953 3 -47 8 -100 11 -117 l5 -33 -254 0 -254 0
-7 83 c-41 499 -223 997 -513 1401 -74 103 -137 181 -197 244 l-41 42 403 403
c385 385 404 402 440 403 91 3 162 75 162 164 0 57 -38 122 -84 144 -50 24
-126 21 -167 -7 -41 -27 -79 -98 -79 -147 0 -33 -24 -59 -399 -434 l-399 -399
-76 67 c-396 345 -876 571 -1397 656 -292 48 -573 49 -869 5 -522 -79 -1025
-312 -1425 -661 l-79 -69 -800 808 c-774 781 -801 809 -802 847 -5 110 -119
188 -221 149 -125 -47 -143 -227 -28 -294 25 -15 57 -25 81 -25 38 0 52 -13
843 -804 l804 -804 -80 -94 c-320 -373 -538 -824 -627 -1300 -22 -116 -46
-303 -46 -359 0 -18 -11 -19 -250 -19 l-250 0 0 53 c0 119 36 366 81 557 52
218 113 398 206 608 l46 102 36 0 c106 1 211 48 284 130 113 125 121 317 21
462 -116 167 -368 199 -534 67 -52 -41 -102 -117 -122 -184 -25 -90 -4 -239
42 -298 l19 -24 -59 -131 c-177 -392 -284 -814 -316 -1249 l-7 -93 -418 0
-418 0 -16 30 c-19 37 -19 93 0 278 48 455 161 896 334 1301 79 184 123 272
228 457 85 149 86 151 131 163 88 23 145 116 122 200 -21 80 -76 121 -162 121
-110 0 -180 -89 -157 -200 7 -30 0 -46 -58 -145 -255 -429 -458 -968 -557
-1480 -26 -138 -66 -433 -66 -491 0 -33 -1 -34 -28 -27 -15 5 -70 8 -122 8
-84 0 -101 -3 -150 -28 -309 -158 -263 -607 70 -693 150 -38 317 27 400 156
l32 50 818 0 818 0 7 -72 c58 -622 298 -1190 689 -1626 31 -34 56 -66 56 -70
0 -4 -79 -86 -175 -182 l-174 -174 -58 63 c-252 277 -472 634 -618 999 -31 78
-60 170 -65 203 -16 111 -72 169 -164 169 -105 0 -170 -68 -163 -171 2 -34 11
-66 23 -85 11 -16 37 -76 58 -134 156 -428 417 -861 718 -1189 l59 -65 -278
-278 -278 -278 -62 0 -62 0 -97 113 c-401 466 -720 1037 -888 1589 l-26 87 21
41 c38 73 16 161 -53 213 -35 26 -114 33 -159 14 -84 -35 -122 -143 -80 -224
19 -38 67 -83 88 -83 4 0 30 -66 57 -147 199 -608 504 -1146 922 -1626 88
-101 89 -103 86 -149 -5 -67 12 -110 62 -156 76 -70 188 -67 259 7 44 45 57
77 57 144 l0 52 558 558 c306 306 562 557 568 557 6 0 21 -12 35 -26 46 -50
225 -188 344 -266 304 -200 657 -342 1020 -412 103 -19 308 -46 357 -46 4 0 8
-113 8 -250 0 -231 -2 -254 -20 -290 -60 -117 21 -257 155 -268 149 -12 253
140 185 273 -18 36 -20 59 -20 287 0 136 4 248 8 248 113 0 425 54 608 105
366 103 762 314 1049 559 55 47 102 86 105 86 3 0 271 -266 595 -591 l590
-590 -45 -41 c-298 -266 -517 -427 -820 -603 -153 -89 -409 -215 -560 -275
l-102 -41 -44 17 c-53 20 -125 14 -163 -13 -37 -28 -71 -91 -71 -133 0 -55 38
-121 85 -147 49 -27 97 -29 150 -4 44 20 65 41 86 85 12 23 36 37 145 81 292
119 548 256 839 450 173 116 385 280 504 389 37 34 79 72 94 86 l28 24 114
-114 114 -113 -19 -38 c-11 -20 -23 -62 -28 -92 -26 -162 57 -321 203 -393 54
-26 70 -29 160 -29 90 0 106 3 160 29 153 76 235 240 201 404 -18 88 -36 123
-98 188 -73 77 -148 108 -263 109 -73 0 -99 -4 -140 -23 l-49 -23 -487 487
-488 488 49 53 c179 196 385 499 518 761 140 276 229 517 297 802 15 63 36
137 47 164 66 161 -112 304 -248 201 -45 -35 -64 -79 -64 -150 0 -100 -79
-387 -171 -618 -133 -334 -347 -682 -581 -943 l-54 -60 -178 173 -179 172 39
46 c308 364 493 696 608 1093 54 188 106 477 106 594 l0 38 668 0 669 0 33
-51 c18 -28 59 -70 91 -94 189 -139 450 -71 553 145 28 58 31 75 31 155 0 81
-4 97 -32 156 -30 61 -97 136 -148 164 -15 9 -20 34 -31 145 -75 754 -336
1483 -757 2112 -187 280 -331 454 -596 718 -367 367 -744 640 -1200 870 -106
53 -117 62 -132 98 -35 86 -141 125 -224 83z m-1477 -1911 c650 -69 1257 -400
1661 -905 427 -533 617 -1261 505 -1935 -153 -927 -842 -1702 -1739 -1954
-752 -211 -1520 -70 -2152 394 -137 101 -398 366 -502 510 -238 329 -385 686
-446 1080 -23 144 -31 489 -15 632 50 462 241 923 531 1283 300 371 753 675
1200 803 146 42 285 72 414 87 137 16 414 18 543 5z"
                />
                <path
                  fill="var(--primary-color)"
                  d="M4914 8640 c-492 -34 -975 -168 -1409 -390 -82 -43 -162 -80 -177
-84 -37 -9 -88 -54 -104 -93 -40 -95 21 -208 123 -229 45 -9 109 13 146 50 66
66 339 195 592 281 195 66 381 111 585 142 160 25 200 27 475 27 337 0 431 -9
709 -69 175 -37 444 -121 557 -172 l66 -31 5 -58 c5 -78 45 -162 105 -222 56
-55 114 -85 200 -102 125 -25 266 31 352 140 54 68 76 137 75 230 -2 100 -31
174 -95 245 -126 137 -327 160 -484 57 l-30 -20 -95 39 c-191 78 -458 158
-685 204 -227 46 -663 72 -911 55z"
                />
                <path
                  fill="var(--primary-color)"
                  d="M9563 4390 c-80 -48 -101 -163 -45 -240 l20 -26 -48 -189 c-78 -309
-192 -614 -338 -904 -73 -145 -80 -155 -112 -164 -116 -30 -163 -155 -97 -256
46 -70 161 -92 226 -44 54 41 66 69 67 156 0 81 1 83 72 224 155 307 280 645
363 978 39 158 44 171 72 190 95 62 93 208 -3 272 -44 30 -131 32 -177 3z"
                />
                <path
                  fill="var(--primary-color)"
                  d="M7095 2383 c-16 -8 -46 -33 -65 -54 -64 -70 -398 -255 -640 -354
-197 -81 -415 -145 -494 -145 -97 0 -166 -69 -166 -167 0 -126 121 -196 242
-140 29 13 78 30 108 37 273 63 690 247 995 437 72 45 142 85 157 88 36 9 85
65 97 113 12 41 1 98 -27 139 -36 56 -144 80 -207 46z"
                />
                <path
                  fill="var(--primary-color)"
                  d="M2524 1391 c-67 -41 -98 -122 -75 -194 26 -78 132 -135 201 -109 21
8 34 5 65 -14 496 -306 1091 -528 1668 -624 186 -30 466 -60 567 -60 48 0 60
-4 81 -26 114 -124 324 -10 279 151 -35 123 -181 165 -272 78 l-27 -26 -143 6
c-320 15 -693 83 -1053 192 -277 84 -693 270 -945 423 -80 48 -95 62 -101 88
-9 49 -46 100 -83 118 -47 22 -123 21 -162 -3z"
                />
              </g>
            </svg>
            <div class="text-surface-900 dark:text-surface-0 text-3xl font-medium mb-4"
              >Chào mừng đến lapxpert Dashboard</div
            >
            <span class="text-muted-color font-medium">Đăng nhập để tiếp tục</span>
          </div>

          <div>
            <label
              for="email1"
              class="block text-surface-900 dark:text-surface-0 text-xl font-medium mb-2"
              >Email</label
            >
            <InputText
              id="email1"
              type="email"
              placeholder="Email address"
              class="w-full md:w-[30rem] mb-8"
              v-model="email"
              :class="{ 'p-invalid': email && !isValidEmail(email) }"
            />
            <small v-if="email && !isValidEmail(email)" class="p-error block mb-4"
              >Email không hợp lệ</small
            >

            <label
              for="password1"
              class="block text-surface-900 dark:text-surface-0 font-medium text-xl mb-2"
              >Mật khẩu</label
            >
            <Password
              id="password1"
              v-model="password"
              placeholder="Password"
              :toggleMask="true"
              class="mb-4"
              fluid
              :feedback="false"
              :class="{ 'p-invalid': password && password.length < 6 }"
            ></Password>
            <small v-if="password && password.length < 6" class="p-error block mb-4"
              >Mật khẩu phải có ít nhất 6 ký tự</small
            >

            <div class="flex items-center justify-between mt-2 mb-8 gap-8">
              <div class="flex items-center">
                <Checkbox v-model="checked" id="rememberme1" binary class="mr-2"></Checkbox>
                <label for="rememberme1">Nhớ tôi</label>
              </div>
              <span class="font-medium no-underline ml-2 text-right cursor-pointer text-primary"
                >Quên mật khẩu?</span
              >
            </div>
            <Button
              label="Sign In"
              class="w-full"
              @click="handleLogin"
              :loading="isLoading"
            ></Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pi-eye {
  transform: scale(1.6);
  margin-right: 1rem;
}

.pi-eye-slash {
  transform: scale(1.6);
  margin-right: 1rem;
}
</style>
