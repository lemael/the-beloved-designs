<script setup lang="ts">
import type { CategoryHero } from '@/features/products/categoryHeroes'

defineProps<{ hero: CategoryHero; targetId: string }>()

function scrollToProducts(targetId: string) {
  document.getElementById(targetId)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <section class="category-hero" :class="`category-hero--${hero.layout ?? 'photo-left'}`">
    <img class="category-hero__photo" :src="`/images/heroes/${hero.image}.jpg`" alt="" />

    <div class="category-hero__content">
      <svg class="category-hero__crown" viewBox="0 0 64 40" aria-hidden="true">
        <defs>
          <linearGradient id="category-hero-gold" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#f6e3a1" />
            <stop offset="0.5" stop-color="#d4a94a" />
            <stop offset="1" stop-color="#9a7428" />
          </linearGradient>
        </defs>
        <path fill="url(#category-hero-gold)" d="M6 14 L19 24 L32 4 L45 24 L58 14 L53 32 H11 Z M11 34 H53 V38 H11 Z" />
        <circle cx="6" cy="12" r="3" fill="url(#category-hero-gold)" />
        <circle cx="32" cy="3" r="3" fill="url(#category-hero-gold)" />
        <circle cx="58" cy="12" r="3" fill="url(#category-hero-gold)" />
      </svg>
      <p class="category-hero__brand">The Beloved Designs</p>
      <h1 class="category-hero__subtitle"><span>{{ hero.subtitle }}</span></h1>
      <span class="category-hero__rule" aria-hidden="true"></span>
      <p class="category-hero__tagline">{{ hero.tagline[0] }}<br />{{ hero.tagline[1] }}</p>
      <svg class="category-hero__swoosh" viewBox="0 0 200 14" aria-hidden="true">
        <path d="M4 12 Q100 -2 196 8" fill="none" stroke="url(#category-hero-gold)" stroke-width="3" stroke-linecap="round" />
      </svg>
      <button type="button" class="category-hero__cta" @click="scrollToProducts(targetId)">
        Acheter maintenant <span aria-hidden="true">→</span>
      </button>
    </div>

    <aside v-if="hero.highlights?.length || hero.signature" class="category-hero__aside">
      <ul v-if="hero.highlights?.length" class="category-hero__highlights">
        <li v-for="item in hero.highlights" :key="item">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="9.5" />
            <path d="M7.5 12.5l3 3 6-7" />
          </svg>
          {{ item }}
        </li>
      </ul>
      <p v-if="hero.signature" class="category-hero__signature">
        <span>{{ hero.signature[0] }}</span>
        <span>{{ hero.signature[1] }}</span>
      </p>
    </aside>
  </section>
</template>

<style scoped>
/* Fond noir avec des reflets dorés (remplace les vagues satinées de la maquette). */
.category-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
  align-items: center;
  min-height: 340px;
  margin-top: var(--spacing-md);
  overflow: hidden;
  border: 1px solid var(--color-gold-dark);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(ellipse 60% 90% at 100% 0%, rgba(212, 169, 74, 0.35), transparent 60%),
    radial-gradient(ellipse 50% 70% at 95% 100%, rgba(212, 169, 74, 0.25), transparent 60%),
    linear-gradient(120deg, #050505 0%, #141008 55%, #050505 100%);
}

.category-hero__photo {
  align-self: stretch;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  /* Fondu vers le texte, à droite. */
  -webkit-mask-image: linear-gradient(90deg, #000 70%, transparent);
  mask-image: linear-gradient(90deg, #000 70%, transparent);
}

.category-hero__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-md);
  text-align: center;
}

.category-hero__crown {
  width: 56px;
  height: 35px;
  filter: drop-shadow(0 2px 6px rgba(212, 169, 74, 0.45));
}

.category-hero__brand {
  margin: 4px 0 0;
  font-family: var(--font-heading);
  font-style: italic;
  font-weight: 700;
  font-size: clamp(1.7rem, 4vw, 3rem);
  line-height: 1.1;
  background: linear-gradient(180deg, #fbeab0 0%, #e2b955 45%, #b8862f 70%, #f1d27c 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 3px 4px rgba(0, 0, 0, 0.6));
}

.category-hero__subtitle {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin: 4px 0 0;
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: clamp(1rem, 2vw, 1.4rem);
  color: var(--color-gold-light);
}

/* typography.css impose la police du texte aux <span> : on la remet ici. */
.category-hero__subtitle span {
  font-family: inherit;
}

.category-hero__subtitle::before,
.category-hero__subtitle::after {
  content: '';
  width: clamp(20px, 4vw, 48px);
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-gold));
}

.category-hero__subtitle::after {
  background: linear-gradient(90deg, var(--color-gold), transparent);
}

.category-hero__rule {
  width: min(80%, 420px);
  height: 2px;
  margin-block: var(--spacing-md);
  background: linear-gradient(90deg, transparent, var(--color-gold-light), transparent);
}

.category-hero__tagline {
  margin: 0;
  font-family: var(--font-heading);
  font-style: italic;
  font-size: clamp(1.2rem, 2.6vw, 1.8rem);
  line-height: 1.25;
  color: #fff;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.7);
}

.category-hero__swoosh {
  width: min(45%, 180px);
  height: 14px;
  margin-top: 4px;
}

.category-hero__cta {
  margin-top: var(--spacing-md);
  padding: 10px 26px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(180deg, #f6dc8f, #d4a94a);
  color: #0b0b0b;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(212, 169, 74, 0.35);
}

.category-hero__cta:hover {
  background: linear-gradient(180deg, #fbe7a8, #e0b65a);
}

/* --- Mise en page « photo au centre » (hommes) : texte | photo | atouts --- */
.category-hero--photo-center {
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.8fr) minmax(0, 0.75fr);
  background:
    radial-gradient(ellipse 35% 80% at 62% 30%, rgba(212, 169, 74, 0.3), transparent 65%),
    radial-gradient(ellipse 40% 70% at 100% 0%, rgba(212, 169, 74, 0.3), transparent 60%),
    linear-gradient(120deg, #050505 0%, #141008 55%, #050505 100%);
}

.category-hero--photo-center .category-hero__photo {
  grid-column: 2;
  grid-row: 1;
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 18%, #000 82%, transparent);
  mask-image: linear-gradient(90deg, transparent, #000 18%, #000 82%, transparent);
}

.category-hero--photo-center .category-hero__content {
  grid-column: 1;
  grid-row: 1;
}

.category-hero__aside {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg) var(--spacing-md);
}

.category-hero__highlights {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.category-hero__highlights li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 1rem;
}

.category-hero__highlights svg {
  flex: none;
  width: 22px;
  height: 22px;
  fill: none;
  stroke: var(--color-gold);
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* « Style sans limites ! » en écriture manuscrite dorée, légèrement inclinée. */
.category-hero__signature {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0;
  transform: rotate(-8deg);
  line-height: 0.95;
}

.category-hero__signature span {
  font-family: var(--font-script);
  font-size: clamp(1.6rem, 3vw, 2.6rem);
  color: var(--color-gold-light);
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.6);
}

.category-hero__signature span:first-child {
  font-size: clamp(2rem, 4vw, 3.2rem);
}

@media (max-width: 1000px) and (min-width: 701px) {
  .category-hero--photo-center {
    grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  }

  .category-hero--photo-center .category-hero__aside {
    display: none;
  }
}

/* Mobile : la photo passe en fond, assombrie, sous le texte. */
@media (max-width: 700px) {
  .category-hero,
  .category-hero--photo-center {
    grid-template-columns: 1fr;
  }

  .category-hero--photo-center .category-hero__photo {
    grid-column: auto;
  }

  .category-hero--photo-center .category-hero__content {
    grid-column: auto;
  }

  .category-hero__aside {
    position: relative;
    padding-top: 0;
  }

  .category-hero__signature {
    display: none;
  }

  .category-hero__photo {
    position: absolute;
    inset: 0;
    opacity: 0.45;
    -webkit-mask-image: none;
    mask-image: none;
  }

  .category-hero__content {
    position: relative;
    padding-block: var(--spacing-xl);
  }
}
</style>
