##request
```
url = "https://api.siliconflow.cn/v1/rerank"

payload = {
    "model": "Qwen/Qwen3-Reranker-8B",
    "instruction": "Please rerank the documents based on the query.",
    "query": "apple",
    "documents": ["apple", "banana", "fruit", "vegetable"],
    "top_n": 5,
    "return_documents": True,
    "max_chunks_per_doc": 123,
    "overlap_tokens": 79
}
headers = {
    "Authorization": "Bearer sk-fvkljvsojrgknsnqftkpnjoxfqvjijitspsvalywcfblvhim",
    "Content-Type": "application/json"
}

response = requests.post(url, json=payload, headers=headers)

print(response.json())
```

##response
```
{
  "id": "019a20ac81e472618b6f8353f7de2686",
  "results": [
    {
      "index": 1,
      "relevance_score": 0.985301673412323
    },
    {
      "index": 3,
      "relevance_score": 0.2598005533218384
    },
    {
      "index": 2,
      "relevance_score": 0.17265193164348602
    },
    {
      "index": 0,
      "relevance_score": 0.041709233075380325
    }
  ],
  "meta": {
    "billed_units": {
      "input_tokens": 9,
      "output_tokens": 0,
      "search_units": 0,
      "classifications": 0
    },
    "tokens": {
      "input_tokens": 9,
      "output_tokens": 0
    }
  }
}
```