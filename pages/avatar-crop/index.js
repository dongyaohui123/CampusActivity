const feedback = require("../../utils/feedback");

const OUTPUT_SIZE = 512;
const MIN_CROP_SIZE = 240;
const BOTTOM_SPACE = 220;
const HORIZONTAL_SPACE = 40;

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

Page({
  data: {
    sourceImage: "",
    ready: false,
    exporting: false,
    cropSize: MIN_CROP_SIZE,
    renderWidth: MIN_CROP_SIZE,
    renderHeight: MIN_CROP_SIZE,
    positionX: 0,
    positionY: 0,
    scale: 1,
    minScale: 1,
    maxScale: 4,
  },

  onLoad(options) {
    const sourceImage = decodeURIComponent(String((options && options.src) || "")).trim();
    if (!sourceImage) {
      feedback.error("未找到待裁剪图片");
      this.navigateBackSafely();
      return;
    }

    this.transformState = {
      x: 0,
      y: 0,
      scale: 1,
    };
    this.imageMeta = null;
    this.initializeCropBox(sourceImage);
  },

  initializeCropBox(sourceImage) {
    const systemInfo = wx.getSystemInfoSync();
    const cropSize = Math.max(
      MIN_CROP_SIZE,
      Math.min(systemInfo.windowWidth - HORIZONTAL_SPACE, systemInfo.windowHeight - BOTTOM_SPACE)
    );

    wx.getImageInfo({
      src: sourceImage,
      success: (res) => {
        const imageWidth = Number(res.width || 0);
        const imageHeight = Number(res.height || 0);
        if (!imageWidth || !imageHeight) {
          feedback.error("读取图片失败");
          this.navigateBackSafely();
          return;
        }

        const imageRatio = imageWidth / imageHeight;
        const renderWidth = imageRatio >= 1 ? cropSize * imageRatio : cropSize;
        const renderHeight = imageRatio >= 1 ? cropSize : cropSize / imageRatio;
        const positionX = (cropSize - renderWidth) / 2;
        const positionY = (cropSize - renderHeight) / 2;

        this.imageMeta = {
          width: imageWidth,
          height: imageHeight,
        };
        this.transformState = {
          x: positionX,
          y: positionY,
          scale: 1,
        };

        this.setData({
          sourceImage,
          ready: true,
          cropSize,
          renderWidth,
          renderHeight,
          positionX,
          positionY,
          scale: 1,
        });
      },
      fail: () => {
        feedback.error("读取图片失败");
        this.navigateBackSafely();
      },
    });
  },

  onTransformChange(event) {
    const detail = (event && event.detail) || {};
    if (Number.isFinite(detail.x)) {
      this.transformState.x = Number(detail.x);
    }
    if (Number.isFinite(detail.y)) {
      this.transformState.y = Number(detail.y);
    }
    if (Number.isFinite(detail.scale)) {
      this.transformState.scale = Number(detail.scale);
    }
  },

  onTransformScale(event) {
    const detail = (event && event.detail) || {};
    if (Number.isFinite(detail.scale)) {
      this.transformState.scale = Number(detail.scale);
    }
    if (Number.isFinite(detail.x)) {
      this.transformState.x = Number(detail.x);
    }
    if (Number.isFinite(detail.y)) {
      this.transformState.y = Number(detail.y);
    }
  },

  onCancelTap() {
    this.navigateBackSafely();
  },

  onConfirmTap() {
    if (!this.data.ready || this.data.exporting || !this.imageMeta) {
      return;
    }

    const scale = clamp(Number(this.transformState.scale || 1), this.data.minScale, this.data.maxScale);
    const renderedWidth = Number(this.data.renderWidth) * scale;
    const renderedHeight = Number(this.data.renderHeight) * scale;
    if (renderedWidth <= 0 || renderedHeight <= 0) {
      feedback.error("裁剪参数无效");
      return;
    }

    const sourceX = clamp((-Number(this.transformState.x || 0) / renderedWidth) * this.imageMeta.width, 0, this.imageMeta.width);
    const sourceY = clamp((-Number(this.transformState.y || 0) / renderedHeight) * this.imageMeta.height, 0, this.imageMeta.height);
    const sourceWidth = clamp((Number(this.data.cropSize) / renderedWidth) * this.imageMeta.width, 1, this.imageMeta.width - sourceX);
    const sourceHeight = clamp((Number(this.data.cropSize) / renderedHeight) * this.imageMeta.height, 1, this.imageMeta.height - sourceY);

    this.setData({ exporting: true });

    const ctx = wx.createCanvasContext("avatarCropCanvas", this);
    ctx.clearRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
    ctx.drawImage(
      this.data.sourceImage,
      sourceX,
      sourceY,
      sourceWidth,
      sourceHeight,
      0,
      0,
      OUTPUT_SIZE,
      OUTPUT_SIZE
    );
    ctx.draw(false, () => {
      wx.canvasToTempFilePath(
        {
          canvasId: "avatarCropCanvas",
          destWidth: OUTPUT_SIZE,
          destHeight: OUTPUT_SIZE,
          width: OUTPUT_SIZE,
          height: OUTPUT_SIZE,
          fileType: "jpg",
          quality: 0.92,
          success: (res) => {
            const tempFilePath = String((res && res.tempFilePath) || "").trim();
            if (!tempFilePath) {
              this.setData({ exporting: false });
              feedback.error("导出裁剪结果失败");
              return;
            }
            const eventChannel = typeof this.getOpenerEventChannel === "function" ? this.getOpenerEventChannel() : null;
            if (eventChannel && typeof eventChannel.emit === "function") {
              eventChannel.emit("cropped", { tempFilePath });
            }
            this.setData({ exporting: false });
            wx.navigateBack();
          },
          fail: () => {
            this.setData({ exporting: false });
            feedback.error("导出裁剪结果失败");
          },
        },
        this
      );
    });
  },

  navigateBackSafely() {
    setTimeout(() => {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        wx.navigateBack();
      }
    }, 120);
  },
});
