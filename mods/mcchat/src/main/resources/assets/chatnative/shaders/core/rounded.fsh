#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec2 Size;
uniform float Pad;
uniform float Radius;
uniform float Mode;
uniform vec4 Skin;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float roundBox(vec2 p, vec2 extent, float r) {
    vec2 q = abs(p) - extent + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

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

void main() {
    vec2 inner = Size - 2.0 * Pad;
    vec2 local = texCoord0 * Size - Pad;
    vec2 unit = local / inner;
    vec4 color = vertexColor;
    float d;

    if (Mode > 1.5) {
        vec2 glyph = unit * 24.0;
        d = polygon(glyph);
    } else {
        d = roundBox(local - inner * 0.5, inner * 0.5, Radius);
        if (Mode > 0.5) {
            vec2 face = vec2(Skin.x, Skin.z) + unit * Skin.w;
            vec2 hat = vec2(Skin.y, Skin.z) + unit * Skin.w;
            vec4 base = texture(Sampler0, face);
            vec4 over = texture(Sampler0, hat);
            color = vec4(mix(base.rgb, over.rgb, over.a), 1.0) * vertexColor;
        }
    }

    float width = max(fwidth(d), 1e-4);
    float coverage = clamp(0.5 - d / width, 0.0, 1.0);
    fragColor = vec4(color.rgb, color.a * coverage) * ColorModulator;
}
