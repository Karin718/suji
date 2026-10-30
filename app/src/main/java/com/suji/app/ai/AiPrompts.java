package com.suji.app.ai;

/** 让 AI 把"原始资料"整理成卡片的提示词。 */
public final class AiPrompts {

    public static final String MODE_AUTO = "auto";
    public static final String MODE_QA = "qa";
    public static final String MODE_TERM = "term";
    public static final String MODE_CLOZE = "cloze";

    private AiPrompts() {
    }

    public static String system() {
        return "你是一位很懂记忆规律的背诵资料整理助手。你的工作是把用户给的原始资料，"
                + "拆成一张张适合间隔重复复习的卡片。你只输出 JSON，不写解释，不使用 Markdown 代码块。";
    }

    public static String user(String raw, String mode, int maxCards) {
        StringBuilder sb = new StringBuilder();
        sb.append("请把下面的原始资料整理成背诵卡片。\n\n");

        if (MODE_QA.equals(mode)) {
            sb.append("形式：问答。front 是一个明确的问题（可以直接问，也可以是「XX 的下一句是？」），back 是准确答案。\n");
        } else if (MODE_TERM.equals(mode)) {
            sb.append("形式：术语解释。front 是术语 / 概念 / 词条，back 是简明准确的解释，不超过 3 句话。\n");
        } else if (MODE_CLOZE.equals(mode)) {
            sb.append("形式：填空。front 是原句，把最关键的词或短语替换成「____」，只挖一处；back 是被挖掉的内容。\n");
        } else {
            sb.append("形式：自动判断。原文是词汇表就做术语解释，是知识点就做问答，是诗词 / 条文就做「下一句」式问答。\n");
        }

        sb.append("\n要求：\n");
        sb.append("1. 一张卡只考一个点，不要塞进多个知识点；\n");
        sb.append("2. front 简洁，尽量不超过 40 个字；back 准确、完整，不超过 200 个字；\n");
        sb.append("3. 忠实于原文，不要自己编造原文里没有的事实；原文有错别字可以顺手改正；\n");
        sb.append("4. 合并重复内容，删掉寒暄、页码、目录等无关文字；\n");
        sb.append("5. 最多生成 ").append(Math.max(1, maxCards)).append(" 张卡；内容多时优先保留最值得背的；\n");
        sb.append("6. note 可以放记忆钩子、易错点、例句，没有就留空字符串；tags 用逗号分隔，没有就留空字符串；\n");
        sb.append("7. 全部使用简体中文（原文是外语单词时，front 保留原文）。\n\n");

        sb.append("严格按下面的 JSON 结构输出（不要输出任何其他文字，不要代码块）：\n");
        sb.append("[{\"front\":\"正面\",\"back\":\"背面\",\"note\":\"备注\",\"tags\":\"标签\"}]\n\n");

        sb.append("原始资料：\n");
        sb.append("<<<\n").append(raw == null ? "" : raw.trim()).append("\n>>>\n");
        return sb.toString();
    }

    /** 用于「测试连接」的一句极短请求。 */
    public static String pingUser() {
        return "请只回复两个字：可用";
    }
}
