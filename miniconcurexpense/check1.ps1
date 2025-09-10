$pod = kubectl -n miniconcur get pod -l app=frontend -o jsonpath='{.items[0].metadata.name}'
kubectl -n miniconcur exec -it $pod -- grep -n 'localhost:30080' /usr/share/nginx/html/assets/index-*.js
