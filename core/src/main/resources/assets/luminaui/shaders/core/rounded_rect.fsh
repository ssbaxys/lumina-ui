#version 150

uniform vec4 ColorModulator;
// Половина размера фигуры в GUI-пикселях.
uniform vec2 HalfSize;
// Радиусы углов: x = низ-право, y = верх-право, z = низ-лево, w = верх-лево.
uniform vec4 Radii;
// x = толщина обводки (0 = заливка), y = мягкость края (радиус размытия тени, 0 = обычное сглаживание).
uniform vec2 Params;

in vec2 localPos;
in vec4 vertexColor;

out vec4 fragColor;

// Signed distance до скруглённого прямоугольника (Inigo Quilez).
float roundedBoxSdf(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    float d = roundedBoxSdf(localPos, HalfSize, Radii);
    if (Params.x > 0.0) {
        d = abs(d + Params.x * 0.5) - Params.x * 0.5;
    }
    float alpha;
    if (Params.y > 0.0) {
        alpha = 1.0 - smoothstep(-Params.y, Params.y, d);
    } else {
        // Сглаживание ровно в один физический пиксель при любом GUI scale.
        float aa = max(fwidth(d), 1e-4);
        alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    }
    vec4 color = vertexColor * ColorModulator;
    if (color.a * alpha < 0.002) {
        discard;
    }
    fragColor = vec4(color.rgb, color.a * alpha);
}
