{{- define "hl7-v2-connector.name" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "hl7-v2-connector.labels" -}}
app.kubernetes.io/name: {{ include "hl7-v2-connector.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
