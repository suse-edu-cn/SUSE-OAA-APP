import json
import urllib.parse

file_path = "qfhy.suse.edu.cn新.har"

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

entries = data['log']['entries']

# 1) /sso/login requests
print("--- 1) Requests containing 'uias.suse.edu.cn/sso/login' ---")
for i, entry in enumerate(entries):
    req = entry['request']
    res = entry['response']
    url = req['url']
    if 'uias.suse.edu.cn/sso/login' in url:
        redirect_url = next((h['value'] for h in res['headers'] if h['name'].lower() == 'location'), '-')
        print(f"Index: {i} | {req['method']} | {url} | {res['status']} | {redirect_url}")

# 2) POST /sso/login
print("\n--- 2) POST requests to '/sso/login' ---")
for i, entry in enumerate(entries):
    req = entry['request']
    if req['method'] == 'POST' and '/sso/login' in req['url']:
        post_data = req.get('postData', {}).get('text', '-')
        fields = []
        if post_data != '-':
            try:
                parsed = urllib.parse.parse_qs(post_data)
                fields = list(parsed.keys())
            except: pass
        print(f"Index: {i}")
        print(f"Data: {post_data}")
        print(f"Fields: {fields}")

# 3) SMS
print("\n--- 3) SMS/Verification ---")
sms_keywords = ['sms', 'message', 'mobile', 'phone', 'code', 'verify', 'send', '短信', '验证']
for i, entry in enumerate(entries):
    req = entry['request']
    res = entry['response']
    url = req['url'].lower()
    post_data = req.get('postData', {}).get('text', '').lower()
    if any(k in url for k in sms_keywords) or any(k in post_data for k in sms_keywords):
        body = res.get('content', {}).get('text', '')[:400]
        print(f"Index: {i} | {req['method']} | {req['url']} | {res['status']}")
        print(f"PostData: {req.get('postData', {}).get('text', '-')}")
        print(f"Response: {body}\n")

# 4) RememberMe
print("\n--- 4) RememberMe ---")
rm_keywords = ['rememberMe', 'autoLogin', 'days', 'CASTGC', 'TGC', 'remember', 'keepLogin']
for i, entry in enumerate(entries):
    req = entry['request']
    res = entry['response']
    headers = {h['name'].lower(): h['value'] for h in req['headers'] + res['headers']}
    if any(k.lower() in str(req['url'] + req.get('postData', {}).get('text', '') + str(headers)).lower() for k in rm_keywords):
        print(f"Index: {i} | {req['url']}")
        for h in req['headers']:
            if h['name'].lower() in ['cookie', 'referer']: print(f"  Req {h['name']}: {h['value']}")
        for h in res['headers']:
            if h['name'].lower() in ['set-cookie', 'location']: print(f"  Res {h['name']}: {h['value']}")

# 5) Chain
print("\n--- 5) Chain ---")
targets = ['/site/appware/system/sso/login', '/xg/app/qddk/admin', '/site/qddk/qdrw/api/myList.rst']
for t in targets:
    for e in entries:
        if t in e['request']['url']:
            print(f"URL: {e['request']['url']}")
            for h in e['request']['headers']:
                if h['name'].lower() in ['cookie', 'referer']: print(f"  Req {h['name']}: {h['value']}")
            for h in e['response']['headers']:
                if h['name'].lower() == 'set-cookie': print(f"  Res Set-Cookie: {h['value']}")
