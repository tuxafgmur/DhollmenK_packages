varying lowp vec4 color;
varying float hueAdjust;

void main() {
    color = ATTRIB_color;
    hueAdjust = ATTRIB_hue;
    gl_Position = vec4(ATTRIB_position.x + ATTRIB_offsetX/3.5, ATTRIB_position.y, 0.0, 1.0);
}
