#version 430 core
#extension GL_ARB_shader_draw_parameters : require

layout (location = 0) in vec3 vertex;

struct Taxi {
    vec4 color;
    vec2 position;
};

layout(std430, binding = 1) buffer instanceData { Taxi taxis[]; };

uniform mat4 projection;

out vec4 pColor;

void main() {
    Taxi taxi = taxis[gl_BaseInstanceARB + gl_InstanceID];

    pColor = vec4(taxi.color.rgb, 1.0);
    gl_Position = projection * vec4(taxi.position + vertex.xy, 0.0, 1.0);
}