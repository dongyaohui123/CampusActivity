const { login, registerUser, wechatLogin } = require("../../utils/api");
const { setLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

const PHONE_REGEX = /^1\d{10}$/;

Page({
  data: {
    i18n: {
      navTitle: "登录/注册",
      loginBtn: "登录",
      registerBtn: "注册",
      loginAccountPlaceholder: "请输入用户名或手机号",
      usernamePlaceholder: "请输入用户名",
      phonePlaceholder: "请输入手机号",
      passwordPlaceholder: "请输入密码",
      confirmPasswordPlaceholder: "请再次输入密码",
      nicknamePlaceholder: "请输入昵称",
      rememberMe: "记住我",
      forgotPassword: "忘记密码？",
    },
    activeTab: "login",
    loginAccount: "",
    username: "",
    phone: "",
    password: "",
    nickname: "",
    confirmPassword: "",
    rememberMe: true,
    showLoginPassword: false,
    showRegisterPassword: false,
    showConfirmPassword: false,
    loading: false,
  },

  onLoad(options) {
    const mode = options && options.mode;
    if (mode === "login" || mode === "register") {
      this.setData({ activeTab: mode });
    }
  },

  onTabTap(event) {
    const tab = event.currentTarget.dataset.tab;
    if (!tab || tab === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: tab });
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [field]: event.detail.value });
  },

  onTogglePassword(event) {
    const target = event.currentTarget.dataset.target;
    if (!target) {
      return;
    }
    this.setData({ [target]: !this.data[target] });
  },

  onRememberTap() {
    this.setData({ rememberMe: !this.data.rememberMe });
  },

  onForgotTap() {
    feedback.info("忘记密码功能开发中");
  },

  async onSocialTap(event) {
    const provider = String(event.currentTarget.dataset.provider || "").toLowerCase();
    if (provider !== "wechat") {
      feedback.info("功能开发中");
      return;
    }
    if (this.data.loading) {
      return;
    }
    await this.submitWechatLogin();
  },

  async onSubmitTap() {
    if (this.data.loading) {
      return;
    }
    if (this.data.activeTab === "register") {
      await this.submitRegister();
      return;
    }
    await this.submitLogin();
  },

  async submitLogin() {
    const account = String(this.data.loginAccount || "").trim();
    const password = String(this.data.password || "");

    if (!account || !password) {
      feedback.error("请输入用户名/手机号和密码");
      return;
    }

    this.setData({ loading: true });
    try {
      const user = await login(account, password);
      setLoginUser(user);
      feedback.success("登录成功");
      this.backAfterAuth();
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({ loading: false, password: "" });
    }
  },

  async submitRegister() {
    const nickname = String(this.data.nickname || "").trim();
    const username = String(this.data.username || "").trim();
    const phone = String(this.data.phone || "").trim();
    const password = String(this.data.password || "");
    const confirmPassword = String(this.data.confirmPassword || "");

    if (!nickname || !username || !phone || !password || !confirmPassword) {
      feedback.error("请完整填写昵称、用户名、手机号和密码");
      return;
    }
    if (!PHONE_REGEX.test(phone)) {
      feedback.error("请输入正确的11位手机号");
      return;
    }
    if (password !== confirmPassword) {
      feedback.error("两次输入的密码不一致");
      return;
    }

    this.setData({ loading: true });
    try {
      const user = await registerUser(username, password, nickname, phone);
      setLoginUser(user);
      feedback.success("注册成功并已登录");
      this.backAfterAuth();
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({
        loading: false,
        password: "",
        confirmPassword: "",
      });
    }
  },

  async submitWechatLogin() {
    this.setData({ loading: true });
    try {
      const profile = await this.tryGetWechatProfile();
      let code = await this.getWechatLoginCode();
      try {
        const user = await this.loginWithWechatCode(code, profile);
        setLoginUser(user);
        feedback.success("微信登录成功");
        this.backAfterAuth();
      } catch (e) {
        if (this.shouldRetryWechatCode(e)) {
          code = await this.getWechatLoginCode();
          const user = await this.loginWithWechatCode(code, profile);
          setLoginUser(user);
          feedback.success("微信登录成功");
          this.backAfterAuth();
          return;
        }
        throw e;
      }
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({ loading: false });
    }
  },

  loginWithWechatCode(code, profile) {
    const payload = { code };
    if (profile) {
      if (profile.nickName) {
        payload.nickname = String(profile.nickName).trim();
      }
      if (profile.avatarUrl) {
        payload.avatarUrl = String(profile.avatarUrl).trim();
      }
      if (Number.isInteger(profile.gender) && profile.gender >= 0 && profile.gender <= 2) {
        payload.gender = profile.gender;
      }
    }
    return wechatLogin(payload);
  },

  shouldRetryWechatCode(error) {
    const message = String((error && error.message) || "").toLowerCase();
    if (!message) {
      return false;
    }
    return message.includes("invalid code")
      || message.includes("code been used")
      || message.includes("errcode=40029")
      || message.includes("errcode=40163");
  },

  getWechatLoginCode() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          const code = String((res && res.code) || "").trim();
          if (!code) {
            feedback.error("微信登录失败：未获取到登录凭证");
            reject(new Error("wechat login code missing"));
            return;
          }
          resolve(code);
        },
        fail: () => {
          feedback.error("微信登录失败，请稍后重试");
          reject(new Error("wechat login failed"));
        },
      });
    });
  },

  tryGetWechatProfile() {
    if (typeof wx.getUserProfile !== "function") {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      wx.getUserProfile({
        desc: "用于完善您的个人资料",
        success: (res) => resolve((res && res.userInfo) || null),
        fail: () => resolve(null),
      });
    });
  },

  backAfterAuth() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
      return;
    }
    wx.reLaunch({ url: "/pages/mine/index" });
  },

  onBackTap() {
    this.backAfterAuth();
  },
});
