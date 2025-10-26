##request
```
url = "https://api.siliconflow.cn/v1/chat/completions"

payload = {
    "model": "deepseek-ai/DeepSeek-R1",
    "messages": [
        {
            "role": "user",
            "content": "你好，介绍一下你自己吧"
        }
    ],
    "stream": False,
    "max_tokens": 4096,
    "enable_thinking": False,
    "thinking_budget": 4096,
    "min_p": 0.05,
    "stop": None,
    "temperature": 0.7,
    "top_p": 0.7,
    "top_k": 50,
    "frequency_penalty": 0.5,
    "n": 1,
    "response_format": { "type": "text" },
    "tools": [
        {
            "type": "function",
            "function": {
                "description": "<string>",
                "name": "<string>",
                "parameters": {},
                "strict": False
            }
        }
    ]
}
headers = {
    "Authorization": "Bearer sk-fvkljvsojrgknsnqftkpnjoxfqvjijitspsvalywcfblvhim",
    "Content-Type": "application/json"
}

response = requests.post(url, json=payload, headers=headers)
```

##response
```
{
  "id": "019a20dae1de7d8fbb25d5a488d3a15f",
  "object": "chat.completion",
  "created": 1761487806,
  "model": "deepseek-ai/DeepSeek-R1",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "\n你好呀！👋我是 **DeepSeek-R1**，由中国的「深度求索」团队研发的一款智能助手。我的使命就是用最清晰、最贴心的方式，帮你解答问题、提供灵感、整理信息、完成任务！✨\n\n### 🌟关于我的一些小特点：\n- **知识丰富**：我的知识更新到 **2024年7月**，涵盖科技、教育、文化、生活、编程、写作等各个领域。\n- **文本小能手**：我擅长阅读和处理各种文字内容，比如写报告、润色作文、总结文档、翻译语言等等📄✍️。\n- **免费使用**：目前我是完全免费的！没有收费计划，也没有隐藏功能，放心大胆地用我吧～\n- **支持文档上传**：你可以上传 PDF、Word、PPT、Excel、图片（可识别图中文字）等文件，我可以帮你提取内容、总结重点、翻译、整理信息等📎。\n- **记忆超长上下文**：我能记住长达 **128K 字符** 的对话内容，处理复杂任务、长文档也不在话下！\n- **暂时没有语音和识图能力**：目前我还不能“听”或“看图识别物体”，但能读取图片中的文字内容。\n\n### 💬我能帮你做什么？\n- 学习辅导 📚（数学、英语、历史、编程等）\n- 工作支持 💼（写简历、写周报、做PPT、数据分析）\n- 创意灵感 🎨（写小说、写剧本、写诗歌、设计活动）\n- 生活百科 🍳（美食推荐、旅游攻略、健康小贴士）\n- 代码帮手 💻（Python、Java、前端、SQL……）\n- 情感陪伴 ❤️（聊天、倾诉、解压小树洞）\n\n如果你有任何问题，无论大小，都可以随时告诉我～我超喜欢帮助人的！😊  \n现在，你想了解些什么？或者需要我帮你做点什么呢？",
        "reasoning_content": "你好呀！很高兴认识你！我是 DeepSeek-R1，一个由深度求索公司开发的人工智能助手。我的知识储备更新到2024年7月，可以帮你解答各种问题，无论是学习上的难题、工作上的挑战，还是生活中的小困惑，我都乐意为你提供帮助。\n\n我喜欢用清晰、有条理的方式组织信息，也会尽量让回答显得温暖又有人情味。如果你需要写文章、做报告、翻译内容、写代码、分析数据……我都可以试试看！当然，聊天解闷、分享想法我也很在行。\n\n目前我可以免费使用，支持上传文档（比如PDF、Word、Excel等）来提取和分析内容。不过要注意，我还不支持图片识别，所以纯文本处理是我的强项。\n\n有什么想了解的，尽管告诉我吧！😊\n"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 580,
    "total_tokens": 590,
    "completion_tokens_details": {
      "reasoning_tokens": 169
    }
  },
  "system_fingerprint": ""
}
```
