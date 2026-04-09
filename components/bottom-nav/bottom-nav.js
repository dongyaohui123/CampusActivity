Component({
  properties: {
    active: {
      type: String,
      value: "home",
    },
  },
  methods: {
    onTabTap(event) {
      const tab = event.currentTarget.dataset.tab;
      this.triggerEvent("change", { tab });
    },
  },
});



