Component({
  data: {
    visible: false,
    type: 'info',
    title: '',
    message: '',
    confirmText: '确定',
    animationClass: '',
    autoClose: false,
    duration: 2000,
    _timer: null,
  },

  methods: {
    show(options = {}) {
      // 清除之前的定时器
      if (this.data._timer) {
        clearTimeout(this.data._timer);
      }

      const type = options.type || 'info';
      const autoClose = type === 'success' || type === 'info';

      this.setData({
        visible: true,
        type,
        title: options.title || this.getDefaultTitle(type),
        message: options.message || '',
        confirmText: options.confirmText || '确定',
        animationClass: '',
        autoClose,
        duration: options.duration || 2000,
      });

      // 自动关闭
      if (autoClose) {
        const timer = setTimeout(() => {
          this.hide();
        }, this.data.duration);
        this.setData({ _timer: timer });
      }
    },

    hide() {
      if (this.data._timer) {
        clearTimeout(this.data._timer);
      }
      this.setData({
        visible: false,
        animationClass: '',
        _timer: null,
      });
    },

    getDefaultTitle(type) {
      const titles = {
        success: '成功',
        error: '错误',
        warning: '警告',
        info: '提示',
      };
      return titles[type] || '提示';
    },

    onConfirm() {
      this.hide();
      this.triggerEvent('confirm');
    },

    onMaskTap() {
      // 错误和警告不允许点击遮罩关闭
      if (this.data.type === 'error' || this.data.type === 'warning') {
        return;
      }
      this.hide();
    },

    noop() {
      // 阻止事件冒泡
    },
  },
});
