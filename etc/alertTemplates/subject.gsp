<%
priority = "";
for (i = 0; i < alertDef.priority; i++) {
    priority += '!'
}
%>[HQ] ${priority} - ${alertDef.name} ${resource.name} ${status}
