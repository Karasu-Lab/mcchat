#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

#ifdef FACE
uniform sampler2D Sampler0;
#endif

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 unitCoord;
layout(location = 2) in vec2 faceCoord;
layout(location = 3) in vec2 guiPos;
layout(location = 4) in float radius;

layout(location = 0) out vec4 fragColor;

float roundBox(vec2 p, vec2 extent, float r) {
    vec2 q = abs(p) - extent + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

#ifdef ICON
float polygon(vec2 p) {
    vec2 arrow[12] = vec2[12](
        vec2(10.0, 9.0), vec2(10.0, 5.0), vec2(3.0, 12.0), vec2(10.0, 19.0),
        vec2(10.0, 14.9), vec2(13.47, 15.21), vec2(16.44, 16.14), vec2(18.93, 17.67),
        vec2(21.0, 20.0), vec2(19.84, 16.31), vec2(17.75, 13.0), vec2(14.53, 10.44));
    float d = dot(p - arrow[0], p - arrow[0]);
    float s = 1.0;
    for (int i = 0, j = 11; i < 12; j = i, i++) {
        vec2 e = arrow[j] - arrow[i];
        vec2 w = p - arrow[i];
        vec2 b = w - e * clamp(dot(w, e) / dot(e, e), 0.0, 1.0);
        d = min(d, dot(b, b));
        bvec3 c = bvec3(p.y >= arrow[i].y, p.y < arrow[j].y, e.x * w.y > e.y * w.x);
        if (all(c) || all(not(c))) {
            s = -s;
        }
    }
    return s * sqrt(d);
}
#endif

void main() {
    vec2 inner = vec2(
        abs(dFdx(guiPos.x) / dFdx(unitCoord.x)),
        abs(dFdy(guiPos.y) / dFdy(unitCoord.y)));
    vec2 local = unitCoord * inner;
    vec4 color = vertexColor;
    float d;

#ifdef ICON
    d = polygon(unitCoord * 24.0) * (inner.x / 24.0);
#else
    d = roundBox(local - inner * 0.5, inner * 0.5, radius);
#endif

#ifdef FACE
    vec4 base = texture(Sampler0, faceCoord);
    vec4 over = texture(Sampler0, faceCoord + vec2(HAT_DX, 0.0));
    color = vec4(mix(base.rgb, over.rgb, over.a), 1.0) * vertexColor;
#endif

    float width = max(fwidth(d), 1e-4);
    float coverage = clamp(0.5 - d / width, 0.0, 1.0);
    color = vec4(color.rgb, color.a * coverage) * ColorModulator;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color;
}
