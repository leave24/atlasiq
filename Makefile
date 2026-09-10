SHELL := /bin/bash
CLUSTER := atlasiq-local
NAMESPACE := atlasiq
BACKEND_IMAGE := atlasiq/backend:local
FRONTEND_IMAGE := atlasiq/frontend:local

.PHONY: cluster ingress build load deploy wait smoke up destroy status

cluster:
	kind get clusters | grep -qx $(CLUSTER) || kind create cluster --config deploy/kind/cluster.yaml

ingress:
	helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx >/dev/null 2>&1 || true
	helm repo update
	helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
		--namespace ingress-nginx --create-namespace \
		--set controller.nodeSelector.ingress-ready=true \
		--set controller.tolerations[0].key=node-role.kubernetes.io/control-plane \
		--set controller.tolerations[0].operator=Exists \
		--set controller.tolerations[0].effect=NoSchedule \
		--set controller.hostPort.enabled=true \
		--set controller.service.type=ClusterIP

build:
	docker build -t $(BACKEND_IMAGE) backend
	docker build -t $(FRONTEND_IMAGE) frontend

load:
	kind load docker-image $(BACKEND_IMAGE) --name $(CLUSTER)
	kind load docker-image $(FRONTEND_IMAGE) --name $(CLUSTER)

deploy:
	kubectl apply -f deploy/kubernetes/base/namespace.yaml
	kubectl apply -f deploy/kubernetes/base/postgres.yaml
	kubectl apply -f deploy/kubernetes/base/apps.yaml
	kubectl apply -f deploy/kubernetes/base/ingress.yaml

wait:
	kubectl -n $(NAMESPACE) rollout status statefulset/postgres --timeout=180s
	kubectl -n $(NAMESPACE) rollout status deployment/atlasiq-backend --timeout=180s
	kubectl -n $(NAMESPACE) rollout status deployment/atlasiq-frontend --timeout=180s

smoke:
	curl --fail --silent --show-error -H 'Host: atlasiq.local' http://127.0.0.1:8080/ >/dev/null
	@echo 'AtlasIQ local stack is reachable at http://atlasiq.local:8080'

up: cluster ingress build load deploy wait smoke

destroy:
	kind delete cluster --name $(CLUSTER)

status:
	kubectl get nodes
	kubectl -n $(NAMESPACE) get pods,svc,ingress,pvc
