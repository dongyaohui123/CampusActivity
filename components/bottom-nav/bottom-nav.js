Component({
  properties: {
    active: {
      type: String,
      value: "home",
    },
  },
  data: {
    labels: {
      home: "\u9996\u9875",
      find: "\u627e\u6d3b\u52a8",
      mine: "\u6211\u7684",
    },
    icons: {
      home: "\u9996",
      find: "\u627e",
      mine: "\u6211",
    },
  },
  methods: {
    onTabTap(event) {
      const tab = event.currentTarget.dataset.tab;
      this.triggerEvent("change", { tab });
    },
  },
});
