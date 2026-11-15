# Multi-stage: compila el ui-shell + las 3 apps y las sirve con nginx.
FROM node:20-alpine AS build
WORKDIR /ws
COPY package.json ./
COPY packages ./packages
COPY apps ./apps
RUN npm install
RUN npm run build:shell
RUN npm run build:catalog && npm run build:subscription && npm run build:security

FROM nginx:alpine
# seguridad en la raíz; catálogo y suscripción en subcarpetas (coinciden con sus base paths)
COPY --from=build /ws/apps/security/dist        /usr/share/nginx/html
COPY --from=build /ws/apps/catalog/dist         /usr/share/nginx/html/catalog
COPY --from=build /ws/apps/subscription/dist    /usr/share/nginx/html/subscription
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
