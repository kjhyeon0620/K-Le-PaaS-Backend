FROM node:20-bullseye-slim AS runner
# Runtime-only image. Expect host to run `npm ci && npm run build` beforehand.
WORKDIR /app
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0

# Copy prebuilt Next.js standalone output from host
COPY .next/standalone ./
COPY .next/static ./.next/static
## Note: public may not exist; runtime does not require it if unused

EXPOSE 3000
CMD ["node", "server.js"]


