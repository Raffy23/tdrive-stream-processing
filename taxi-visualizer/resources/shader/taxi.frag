#version 430 core

in vec4 pColor;

out vec4 FragColor;

void main()
{
    FragColor = vec4(pColor.rgb, 1.0f);
}