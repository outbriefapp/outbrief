// @ts-check
import starlight from "@astrojs/starlight";
import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://outbriefapp.com",
  integrations: [
    starlight({
      title: "OutBrief",
      description: "AI Agent 干完活，会给你打电话：以视频来电的形式做语音 + 卡片汇报。",
      logo: { src: "./src/assets/logo.svg" },
      favicon: "/favicon.svg",
      defaultLocale: "root",
      locales: {
        root: { label: "简体中文", lang: "zh-CN" },
        en: { label: "English", lang: "en" },
      },
      social: [
        { icon: "github", label: "GitHub", href: "https://github.com/outbriefapp" },
        { icon: "discord", label: "Discord", href: "https://discord.gg/TG8qHzdv6" },
      ],
      editLink: { baseUrl: "https://github.com/outbriefapp/outbrief-website/edit/main/" },
      lastUpdated: true,
      customCss: ["./src/styles/custom.css"],
      sidebar: [
        {
          label: "开始",
          translations: { en: "Start" },
          items: ["docs/overview", "docs/quick-start", "download", "changelog"],
        },
        {
          label: "使用指南",
          translations: { en: "Guides" },
          items: [
            "docs/guides/pairing",
            "docs/guides/calls",
            "docs/guides/multica",
            "docs/guides/background-calls",
            "docs/guides/llm",
            "docs/guides/voice",
            "docs/guides/privacy",
          ],
        },
        {
          label: "安装与部署",
          translations: { en: "Install & deploy" },
          items: ["docs/app", "docs/daemon", "docs/server", "docs/development"],
        },
        {
          label: "参考",
          translations: { en: "Reference" },
          collapsed: true,
          items: [
            "docs/reference/server-api",
            "docs/reference/daemon-api",
            "docs/reference/app-source",
          ],
        },
        {
          label: "架构决策（ADR）",
          translations: { en: "Decisions (ADR)" },
          collapsed: true,
          items: [{ autogenerate: { directory: "docs/adr" } }],
        },
      ],
    }),
  ],
});
